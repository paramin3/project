package com.taekwondogym.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "achievements")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255, nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    private LocalDateTime createdAt;
    @ElementCollection
    @CollectionTable(name = "achievement_images", joinColumns = @JoinColumn(name = "achievement_id"))
    @Column(name = "image_path")
    private List<String> imagePaths;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }
}
