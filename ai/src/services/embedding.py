import hashlib
import logging
import math
import re

import torch
import torch.nn.functional as F
from transformers import AutoModel, AutoTokenizer


logger = logging.getLogger(__name__)


class EmbeddingService:
    def __init__(
        self,
        model_name: str = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        dimensions: int = 128,
        device: str = "cpu",
    ):
        self.model_name = model_name
        self.dimensions = dimensions
        self.device = self._resolve_device(device)
        self._tokenizer = None
        self._model = None
        self._projection = None
        self._use_fallback = False

    def generate(self, text: str) -> list[float]:
        normalized = (text or "").strip()
        if not normalized:
            return [0.0] * self.dimensions

        if self._ensure_model_loaded():
            return self._generate_transformer_embedding(normalized)

        return self._generate_hash_embedding(normalized)

    def _ensure_model_loaded(self) -> bool:
        if self._use_fallback:
            return False
        if self._model is not None and self._tokenizer is not None and self._projection is not None:
            return True

        try:
            self._tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            self._model = AutoModel.from_pretrained(self.model_name)
            self._model.to(self.device)
            self._model.eval()
            self._projection = self._build_projection(self._model.config.hidden_size)
            logger.info(
                "Loaded embedding model '%s' on %s with projected dimension %s",
                self.model_name,
                self.device,
                self.dimensions,
            )
            return True
        except Exception as exc:
            logger.warning(
                "Falling back to hash embedding because model '%s' could not be loaded: %s",
                self.model_name,
                exc,
            )
            self._use_fallback = True
            self._tokenizer = None
            self._model = None
            self._projection = None
            return False

    def _generate_transformer_embedding(self, text: str) -> list[float]:
        encoded = self._tokenizer(
            text,
            padding=True,
            truncation=True,
            max_length=256,
            return_tensors="pt",
        )
        encoded = {key: value.to(self.device) for key, value in encoded.items()}

        with torch.inference_mode():
            outputs = self._model(**encoded)
            pooled = self._mean_pool(outputs.last_hidden_state, encoded["attention_mask"])
            projected = pooled @ self._projection
            normalized = F.normalize(projected, p=2, dim=1)

        return normalized.squeeze(0).detach().cpu().tolist()

    def _build_projection(self, hidden_size: int) -> torch.Tensor:
        digest = hashlib.sha256(self.model_name.encode("utf-8")).digest()
        seed = int.from_bytes(digest[:8], "big")
        generator = torch.Generator(device="cpu")
        generator.manual_seed(seed)
        projection = torch.randn(hidden_size, self.dimensions, generator=generator, dtype=torch.float32)
        projection = F.normalize(projection, p=2, dim=0)
        return projection.to(self.device)

    @staticmethod
    def _mean_pool(last_hidden_state: torch.Tensor, attention_mask: torch.Tensor) -> torch.Tensor:
        mask = attention_mask.unsqueeze(-1).expand(last_hidden_state.size()).float()
        masked = last_hidden_state * mask
        summed = masked.sum(dim=1)
        counts = mask.sum(dim=1).clamp(min=1e-9)
        return summed / counts

    @staticmethod
    def _resolve_device(device: str) -> str:
        requested = (device or "cpu").strip().lower()
        if requested == "cuda" and torch.cuda.is_available():
            return "cuda"
        return "cpu"

    def _generate_hash_embedding(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        tokens = re.findall(r"\w+", text.lower())
        if not tokens:
            return vector

        for token in tokens:
            digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
            index = int(digest[:8], 16) % self.dimensions
            weight = 1.0 + (int(digest[8:12], 16) % 100) / 100.0
            vector[index] += weight

        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector

        return [value / norm for value in vector]
