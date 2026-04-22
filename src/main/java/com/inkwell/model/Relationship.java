package com.inkwell.model;

public class Relationship {
    private int id;
    private Integer sourceCharacterId;
    private Integer targetCharacterId;
    private Integer storyId;
    private String type;
    private String description;
    private String createdAt;

    public Relationship() {
    }

    public Relationship(int id, Integer sourceCharacterId, Integer targetCharacterId, Integer storyId,
                        String type, String description, String createdAt) {
        this.id = id;
        this.sourceCharacterId = sourceCharacterId;
        this.targetCharacterId = targetCharacterId;
        this.storyId = storyId;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getSourceCharacterId() {
        return sourceCharacterId;
    }

    public void setSourceCharacterId(Integer sourceCharacterId) {
        this.sourceCharacterId = sourceCharacterId;
    }

    public Integer getTargetCharacterId() {
        return targetCharacterId;
    }

    public void setTargetCharacterId(Integer targetCharacterId) {
        this.targetCharacterId = targetCharacterId;
    }

    public Integer getStoryId() {
        return storyId;
    }

    public void setStoryId(Integer storyId) {
        this.storyId = storyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
