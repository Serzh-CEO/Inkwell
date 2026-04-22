package com.inkwell.controller;

import com.inkwell.database.CharacterRepository;
import com.inkwell.database.DatabaseManager;
import com.inkwell.database.GroupRepository;
import com.inkwell.database.ProjectRepository;
import com.inkwell.database.RelationshipRepository;
import com.inkwell.database.StoryRepository;
import com.inkwell.database.TagRepository;
import com.inkwell.model.Character;
import com.inkwell.model.Group;
import com.inkwell.model.Project;
import com.inkwell.model.Relationship;
import com.inkwell.model.Story;
import com.inkwell.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class InkwellService {
    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;
    private final ProjectRepository projectRepository;
    private final TagRepository tagRepository;
    private final RelationshipRepository relationshipRepository;
    private final GroupRepository groupRepository;

    public InkwellService() {
        DatabaseManager databaseManager = new DatabaseManager();
        this.storyRepository = new StoryRepository(databaseManager);
        this.characterRepository = new CharacterRepository(databaseManager);
        this.projectRepository = new ProjectRepository(databaseManager);
        this.tagRepository = new TagRepository(databaseManager);
        this.relationshipRepository = new RelationshipRepository(databaseManager);
        this.groupRepository = new GroupRepository(databaseManager);
    }

    // ── Projects ─────────────────────────────────────────────────────────────

    public Project createProject(Project project)  { return projectRepository.create(project); }
    public List<Project> getProjects()             { return projectRepository.getAll(); }
    public boolean deleteProject(int id)           { return projectRepository.delete(id); }

    // ── Stories ──────────────────────────────────────────────────────────────

    public Story createStory(Story story)                          { return storyRepository.create(story); }
    public Story getStory(int id)                                  { return storyRepository.getById(id); }
    public List<Story> getStoriesByProject(int projectId)          { return storyRepository.getAllByProjectId(projectId); }
    public List<Story> searchStories(int projectId, String query)  { return storyRepository.searchByProject(projectId, query); }
    public boolean updateStory(Story story)                        { return storyRepository.update(story); }
    public boolean deleteStory(int id)                             { return storyRepository.delete(id); }

    public List<Story> filterStoriesByTag(String tagName) {
        List<Integer> storyIds = tagRepository.getStoryIdsByTagName(tagName);
        List<Story> stories = new ArrayList<>();
        for (Integer id : storyIds) {
            Story story = storyRepository.getById(id);
            if (story != null) stories.add(story);
        }
        return stories;
    }

    // ── Characters ───────────────────────────────────────────────────────────

    public Character createCharacter(Character character)              { return characterRepository.create(character); }
    public Character getCharacter(int id)                              { return characterRepository.getById(id); }
    public List<Character> getCharactersByProject(int projectId)       { return characterRepository.getAllByProjectId(projectId); }
    public List<Character> getCharactersByStory(int storyId)           { return characterRepository.getByStoryId(storyId); }
    public boolean updateCharacter(Character character)                { return characterRepository.update(character); }
    public boolean deleteCharacter(int id)                             { return characterRepository.delete(id); }

    // ── Tags ─────────────────────────────────────────────────────────────────

    public List<Tag> getTags()                                         { return tagRepository.getAll(); }
    public List<Tag> getStoryTags(int storyId)                         { return tagRepository.getByStoryId(storyId); }
    public void setStoryTags(int storyId, List<String> tagNames)       { tagRepository.setStoryTags(storyId, tagNames); }

    // ── Relationships ────────────────────────────────────────────────────────

    public Relationship createRelationship(Relationship r)             { return relationshipRepository.create(r); }
    public List<Relationship> getRelationships()                       { return relationshipRepository.getAll(); }
    public List<Relationship> getRelationshipsByCharacter(int id)      { return relationshipRepository.getByCharacterId(id); }
    public List<Relationship> getRelationshipsByStory(int id)          { return relationshipRepository.getByStoryId(id); }
    public boolean updateRelationship(Relationship r)                  { return relationshipRepository.update(r); }
    public boolean deleteRelationship(int id)                          { return relationshipRepository.delete(id); }

    // ── Groups ───────────────────────────────────────────────────────────────

    public Group createGroup(Group group)                              { return groupRepository.create(group); }
    public Group getGroup(int id)                                      { return groupRepository.getById(id); }
    public List<Group> getGroupsByProject(int projectId, String type)  { return groupRepository.getAllByProjectId(projectId, type); }
    public boolean updateGroup(Group group)                            { return groupRepository.update(group); }
    public boolean updateGroupCollapsed(int groupId, boolean collapsed){ return groupRepository.updateCollapsed(groupId, collapsed); }
    public boolean deleteGroup(int id)                                 { return groupRepository.delete(id); }
    public void setGroupMembers(int groupId, List<Integer> memberIds)  { groupRepository.setMembers(groupId, memberIds); }
    public Integer getGroupIdForMember(int memberId, String type)      { return groupRepository.getGroupIdForMember(memberId, type); }
}
