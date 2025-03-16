package com.ll.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.ll.backend.post.PostDto.postDto;

@Service
@RequiredArgsConstructor
class PostService {
    private final PostRepository postRepository;

    public List<PostDto> getAllPosts() {
        List<Post> postList = postRepository.findAll();
        return postList.stream().map(PostDto::postDto).toList();
    }

    public PostDto getPostById(Long id) {
        return postDto(postRepository.findById(id).orElseThrow());
    }

    public PostDto createPost(PostDto dto) {
        Post post = new Post(dto.getTitle(), dto.getContent());
        return postDto(postRepository.save(post));
    }

    public PostDto updatePost(Long id, PostDto newPost) {
        return postRepository.findById(id).map(post -> {
            post.setTitle(newPost.getTitle());
            post.setContent(newPost.getContent());
            return postDto(postRepository.save(post));
        }).orElse(null);
    }
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}