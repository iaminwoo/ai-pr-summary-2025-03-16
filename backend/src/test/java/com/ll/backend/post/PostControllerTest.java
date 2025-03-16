package com.ll.backend.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllPosts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("title1"))
                .andExpect(jsonPath("$[1].title").value("title2"))
                .andExpect(jsonPath("$[2].title").value("title3"));
    }

    @Test
    void testGetPostById() throws Exception {
        Post firstPost = postRepository.findAll().getFirst();  // 첫 번째 게시물 가져오기

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/" + firstPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("title1"))
                .andExpect(jsonPath("$.content").value("content1"));
    }

    @Test
    void testCreatePost() throws Exception {
        PostDto newPostDto = new PostDto("New Title", "New Content");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newPostDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.content").value("New Content"));

        // 총 게시물 개수가 4개인지 확인 (기본 3개 + 추가 1개)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void testUpdatePost() throws Exception {
        Post firstPost = postRepository.findAll().getFirst();
        PostDto updatedPostDto = new PostDto("Updated Title", "Updated Content");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/posts/" + firstPost.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updatedPostDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));
    }

    @Test
    void testDeletePost() throws Exception {
        Post firstPost = postRepository.findAll().getFirst();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/posts/" + firstPost.getId()))
                .andExpect(status().isOk());

        // 총 게시물 개수가 2개로 줄어야 함 (기본 3개 - 삭제 1개)
        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts"))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
