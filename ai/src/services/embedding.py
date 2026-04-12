import hashlib
import math
import re


class EmbeddingService:
    def __init__(self, dimensions: int = 128):
        self.dimensions = dimensions

    def generate(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        normalized = (text or "").strip().lower()
        if not normalized:
            return vector

        tokens = re.findall(r"\w+", normalized)
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
