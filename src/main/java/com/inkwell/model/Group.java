package com.inkwell.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A named group that can contain stories or characters.
 * type: "story" | "character"
 * color: hex color string, e.g. "#5b7cf6"
 */
public class Group {
    private int id;
    private int projectId;
    private String name;
    private String type;   // "story" or "character"
    private String color;
    private boolean collapsed;
    private String createdAt;

    // Transient — loaded separately
    private final List<Integer> memberIds = new ArrayList<>();

    public Group() {}

    public Group(int id, int projectId, String name, String type, String color,
                 boolean collapsed, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.type = type;
        this.color = color;
        this.collapsed = collapsed;
        this.createdAt = createdAt;
    }

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public int getProjectId()                 { return projectId; }
    public void setProjectId(int projectId)   { this.projectId = projectId; }
    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }
    public String getType()                   { return type; }
    public void setType(String type)          { this.type = type; }
    public String getColor()                  { return color; }
    public void setColor(String color)        { this.color = color; }
    public boolean isCollapsed()              { return collapsed; }
    public void setCollapsed(boolean c)       { this.collapsed = c; }
    public String getCreatedAt()              { return createdAt; }
    public void setCreatedAt(String s)        { this.createdAt = s; }
    public List<Integer> getMemberIds()       { return memberIds; }
    public void addMemberId(int id)           { memberIds.add(id); }

    @Override public String toString()        { return name; }
}
