package com.ll.backend.post;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostDto {
    private String title;
    private String content;

    public static PostDto postDto(Post post) {
        return new PostDto(post.getTitle(), post.getContent());
    }
}
