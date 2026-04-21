package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.application.dto.request.ModifyPostRequest;
import edu.uet.travel_hub.application.dto.response.LikePostResponse;
import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.PostResponse;
import edu.uet.travel_hub.application.mapper.PostMapper;
import edu.uet.travel_hub.application.port.in.CreatePostUseCase;
import edu.uet.travel_hub.application.port.in.GetAllPostsUseCase;
import edu.uet.travel_hub.application.port.in.LikePostUseCase;
import edu.uet.travel_hub.application.port.in.ModifyPostUseCase;
import edu.uet.travel_hub.application.port.in.UnlikePostUseCase;
import edu.uet.travel_hub.application.usecases.ModifyPostService;
import edu.uet.travel_hub.domain.model.PostModel;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final CreatePostUseCase createPostUseCase;
    private final ModifyPostUseCase modifyPostUseCase;
    private final GetAllPostsUseCase getAllPostsUseCase;
    private final LikePostUseCase likePostUseCase;
    private final UnlikePostUseCase unlikePostUseCase;
    private final PostMapper mapper;

    public PostController(CreatePostUseCase createPostUseCase, ModifyPostService modifyPostService,
            GetAllPostsUseCase getAllPostsUseCase, PostMapper mapper, LikePostUseCase likePostUseCase, UnlikePostUseCase unlikePostUseCase) {
        this.createPostUseCase = createPostUseCase;
        this.modifyPostUseCase = modifyPostService;
        this.getAllPostsUseCase = getAllPostsUseCase;
        this.likePostUseCase = likePostUseCase;
        this.unlikePostUseCase = unlikePostUseCase;
        this.mapper = mapper;
    }

    @GetMapping("")
    public ResponseEntity<PaginationResponse<PostResponse>> getAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationResponse<PostModel> posts = this.getAllPostsUseCase.getAll(page, pageSize);
        PaginationResponse<PostResponse> response = new PaginationResponse<>(
                posts.pageNumber(),
                posts.pageSize(),
                posts.totalPages(),
                posts.totalElements(),
                posts.data().stream().map(this.mapper::toDto).toList());
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("")
    public ResponseEntity<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        PostModel post = this.createPostUseCase.create(request);
        PostResponse response = this.mapper.toDto(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> modifyPost(@PathVariable Long postId,
            @RequestBody ModifyPostRequest request) {
        PostModel post = this.modifyPostUseCase.modify(postId, request);
        PostResponse response = this.mapper.toDto(post);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikePostResponse> like(@PathVariable Long postId) {
        LikePostResponse response = this.likePostUseCase.like(postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<LikePostResponse> unlike(@PathVariable Long postId) {
        LikePostResponse response = this.unlikePostUseCase.unlike(postId);
        return ResponseEntity.ok(response);
    }
}
