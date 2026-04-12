package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.ModifyPostRequest;
import edu.uet.travel_hub.domain.model.PostModel;

public interface ModifyPostUseCase {
    PostModel modify(Long id, ModifyPostRequest request);
}
