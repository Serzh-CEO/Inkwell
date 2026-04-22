package com.inkwell.controller;

import com.inkwell.model.Character;
import com.inkwell.model.Group;
import com.inkwell.model.Project;
import com.inkwell.model.Relationship;
import com.inkwell.model.Story;
import com.inkwell.model.Tag;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.HTMLEditor;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainViewController {

    private static final StoryOption NO_STORY_OPTION = new StoryOption(null, "No linked story");
    private static final String ALL_TAGS_OPTION = "All tags";
    private static final String[] GROUP_COLORS = {
        "#5b7cf6","#e07b54","#50c878","#d4af37",
        "#c060c0","#4ecdc4","#ff6b6b","#95a5a6"
    };

    private final InkwellService service = new InkwellService();
    private final ObservableList<Story> stories = FXCollections.observableArrayList();
    private final ObservableList<Character> characters = FXCollections.observableArrayList();
    private final ObservableList<ProjectOption> projects = FXCollections.observableArrayList();
    private final Map<String, GraphNode> graphNodeLookup = new HashMap<>();
    private final List<Group> storyGroups = new ArrayList<>();
    private final List<Group> characterGroups = new ArrayList<>();

    private Integer selectedProjectId;
    private Integer selectedStoryId;
    private Integer selectedCharacterId;
    private double graphScale = 1.0;
    private boolean storyEditorExpanded = false;
    private boolean characterEditorExpanded = false;
    private boolean storyContentExpanded = false;
    private boolean characterContentExpanded = false;

    private HTMLEditor storyContentEditor;
    private HTMLEditor characterDescriptionEditor;
    private VBox storiesListContainer;
    private VBox charactersListContainer;

    // FXML
    @FXML private Label contentTitleLabel;
    @FXML private ComboBox<ProjectOption> projectComboBox;
    @FXML private Button storiesNavButton;
    @FXML private Button charactersNavButton;
    @FXML private Button graphNavButton;
    @FXML private VBox storiesView;
    @FXML private HBox storiesContentArea;
    @FXML private VBox storiesListPanel;
    @FXML private VBox storyEditorPanel;
    @FXML private Button storyBackButton;
    @FXML private Button storyExpandButton;
    @FXML private ListView<Story> storiesListView;
    @FXML private TextField storySearchField;
    @FXML private ComboBox<String> tagFilterComboBox;
    @FXML private TextField storyTitleField;
    @FXML private TextField storyCategoryField;
    @FXML private TextField storyTagsField;
    @FXML private TextArea storySummaryArea;
    @FXML private VBox storyMetaFields;
    @FXML private VBox storyContentEditorContainer;
    @FXML private VBox charactersView;
    @FXML private HBox charactersContentArea;
    @FXML private VBox charactersListPanel;
    @FXML private VBox characterEditorPanel;
    @FXML private Button characterBackButton;
    @FXML private Button characterExpandButton;
    @FXML private ListView<Character> charactersListView;
    @FXML private TextField characterNameField;
    @FXML private ComboBox<StoryOption> characterStoryComboBox;
    @FXML private TextField characterTraitsField;
    @FXML private VBox characterMetaFields;
    @FXML private VBox characterDescEditorContainer;
    @FXML private VBox graphView;
    @FXML private Pane graphPane;

    // ═══════ INIT ════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        buildStoryContentEditor();
        buildCharacterDescEditor();
        buildGroupedListContainers();
        setupGraphInteractions();
        setupProjectSelector();
        loadProjects();
        hideStoryEditor();
        hideCharacterEditor();
        showStoriesView();
    }

    private void buildGroupedListContainers() {
        storiesListContainer = new VBox(4);
        storiesListContainer.setPadding(new Insets(4));
        ScrollPane ss = new ScrollPane(storiesListContainer);
        ss.setFitToWidth(true);
        ss.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(ss, Priority.ALWAYS);
        storiesListView.setVisible(false);
        storiesListView.setManaged(false);
        storiesListPanel.getChildren().add(ss);

        charactersListContainer = new VBox(4);
        charactersListContainer.setPadding(new Insets(4));
        ScrollPane cs = new ScrollPane(charactersListContainer);
        cs.setFitToWidth(true);
        cs.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(cs, Priority.ALWAYS);
        charactersListView.setVisible(false);
        charactersListView.setManaged(false);
        charactersListPanel.getChildren().add(cs);
    }

    // ═══════ RENDER LISTS ════════════════════════════════════════════════

    private void renderStoryList() {
        storiesListContainer.getChildren().clear();
        if (selectedProjectId == null) return;
        Set<Integer> grouped = storyGroups.stream().flatMap(g -> g.getMemberIds().stream()).collect(Collectors.toSet());
        for (Group g : storyGroups) {
            List<Story> members = stories.stream().filter(s -> g.getMemberIds().contains(s.getId())).collect(Collectors.toList());
            storiesListContainer.getChildren().add(buildGroupSection(g, members,
                o -> onStoryRowClick((Story) o),
                this::onStoryGroupRename, this::onStoryGroupDelete,
                this::onStoryGroupRecolor, this::onManageStoryGroupMembers, "story"));
        }
        List<Story> ungrouped = stories.stream().filter(s -> !grouped.contains(s.getId())).collect(Collectors.toList());
        if (!ungrouped.isEmpty() || storyGroups.isEmpty()) {
            if (!storyGroups.isEmpty()) {
                Label lbl = new Label("UNGROUPED");
                lbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#5a6278;-fx-padding:12 8 4 8;");
                storiesListContainer.getChildren().add(lbl);
            }
            for (Story s : ungrouped) storiesListContainer.getChildren().add(buildStoryRow(s));
        }
    }

    private void renderCharacterList() {
        charactersListContainer.getChildren().clear();
        if (selectedProjectId == null) return;
        Set<Integer> grouped = characterGroups.stream().flatMap(g -> g.getMemberIds().stream()).collect(Collectors.toSet());
        for (Group g : characterGroups) {
            List<Character> members = characters.stream().filter(c -> g.getMemberIds().contains(c.getId())).collect(Collectors.toList());
            charactersListContainer.getChildren().add(buildGroupSection(g, members,
                o -> onCharacterRowClick((Character) o),
                this::onCharacterGroupRename, this::onCharacterGroupDelete,
                this::onCharacterGroupRecolor, this::onManageCharacterGroupMembers, "character"));
        }
        List<Character> ungrouped = characters.stream().filter(c -> !grouped.contains(c.getId())).collect(Collectors.toList());
        if (!ungrouped.isEmpty() || characterGroups.isEmpty()) {
            if (!characterGroups.isEmpty()) {
                Label lbl = new Label("UNGROUPED");
                lbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#5a6278;-fx-padding:12 8 4 8;");
                charactersListContainer.getChildren().add(lbl);
            }
            for (Character c : ungrouped) charactersListContainer.getChildren().add(buildCharacterRow(c));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> VBox buildGroupSection(Group group, List<T> members,
            Consumer<Object> onItemClick, Consumer<Group> onRename,
            Consumer<Group> onDelete, Consumer<Group> onRecolor,
            Consumer<Group> onManage, String type) {

        Color groupColor = parseColor(group.getColor());
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color:#1c1f2a;-fx-background-radius:8;" +
            "-fx-border-color:" + group.getColor() + " transparent transparent transparent;" +
            "-fx-border-width:0 0 0 3;-fx-border-radius:0 8 8 0;"
        );

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(7, 10, 7, 10));

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5, groupColor);
        Label nameLabel = new Label(group.getName());
        nameLabel.setStyle("-fx-text-fill:#d0d4e8;-fx-font-weight:700;-fx-font-size:12px;");
        Label badge = new Label(String.valueOf(members.size()));
        badge.setStyle("-fx-background-color:#2b3142;-fx-text-fill:#8e95a8;-fx-font-size:10px;-fx-padding:1 6 1 6;-fx-background-radius:10;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label chevron = new Label(group.isCollapsed() ? "▶" : "▼");
        chevron.setStyle("-fx-text-fill:#5a6278;-fx-font-size:10px;");
        Button menuBtn = new Button("⋯");
        menuBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#5a6278;-fx-font-size:13px;-fx-padding:0 4 0 4;-fx-cursor:hand;");
        menuBtn.setOnMouseEntered(e -> menuBtn.setStyle("-fx-background-color:#2b3142;-fx-text-fill:#c8cfe8;-fx-font-size:13px;-fx-padding:0 4 0 4;-fx-cursor:hand;-fx-background-radius:4;"));
        menuBtn.setOnMouseExited(e -> menuBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#5a6278;-fx-font-size:13px;-fx-padding:0 4 0 4;-fx-cursor:hand;"));
        ContextMenu ctx = buildGroupContextMenu(group, onRename, onDelete, onRecolor, onManage);
        menuBtn.setOnAction(e -> ctx.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0));
        menuBtn.setOnMouseClicked(e -> e.consume());
        header.getChildren().addAll(dot, nameLabel, badge, spacer, menuBtn, chevron);

        VBox membersBox = new VBox(2);
        membersBox.setPadding(new Insets(4, 6, 6, 6));
        membersBox.setVisible(!group.isCollapsed());
        membersBox.setManaged(!group.isCollapsed());
        for (T item : members) {
            Node row = "story".equals(type) ? buildStoryRow((Story) item) : buildCharacterRow((Character) item);
            membersBox.getChildren().add(row);
        }

        header.setOnMouseClicked(e -> {
            boolean nc = !group.isCollapsed();
            group.setCollapsed(nc);
            service.updateGroupCollapsed(group.getId(), nc);
            chevron.setText(nc ? "▶" : "▼");
            membersBox.setVisible(!nc); membersBox.setManaged(!nc);
        });
        card.getChildren().addAll(header, membersBox);
        return card;
    }

    private ContextMenu buildGroupContextMenu(Group group, Consumer<Group> onRename,
            Consumer<Group> onDelete, Consumer<Group> onRecolor, Consumer<Group> onManage) {
        ContextMenu menu = new ContextMenu();
        MenuItem rename = new MenuItem("✏  Rename");
        MenuItem recolor = new MenuItem("🎨  Change color");
        MenuItem manage = new MenuItem("👥  Manage members");
        MenuItem delete = new MenuItem("🗑  Delete group");
        delete.setStyle("-fx-text-fill:#e05252;");
        rename.setOnAction(e -> onRename.accept(group));
        recolor.setOnAction(e -> onRecolor.accept(group));
        manage.setOnAction(e -> onManage.accept(group));
        delete.setOnAction(e -> onDelete.accept(group));
        menu.getItems().addAll(rename, recolor, manage, new SeparatorMenuItem(), delete);
        return menu;
    }

    private HBox buildStoryRow(Story story) {
        boolean sel = story.getId() == (selectedStoryId != null ? selectedStoryId : -1);
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 12, 7, 12));
        row.setStyle(sel
            ? "-fx-background-color:#252b40;-fx-background-radius:6;-fx-border-color:transparent transparent transparent #5b7cf6;-fx-border-width:0 0 0 3;-fx-cursor:hand;"
            : "-fx-background-radius:6;-fx-cursor:hand;");
        Label icon = new Label("📄"); icon.setStyle("-fx-font-size:11px;");
        Label title = new Label(story.getTitle());
        title.setStyle("-fx-text-fill:" + (sel ? "#a0b4fc" : "#c8cfe8") + ";-fx-font-size:13px;" + (sel ? "-fx-font-weight:700;" : ""));
        HBox.setHgrow(title, Priority.ALWAYS);
        row.getChildren().addAll(icon, title);
        row.setOnMouseEntered(e -> { if (!sel) row.setStyle("-fx-background-color:#20253a;-fx-background-radius:6;-fx-cursor:hand;"); });
        row.setOnMouseExited(e -> { if (!sel) row.setStyle("-fx-background-radius:6;-fx-cursor:hand;"); });
        row.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) onStoryRowClick(story); });
        return row;
    }

    private HBox buildCharacterRow(Character character) {
        boolean sel = character.getId() == (selectedCharacterId != null ? selectedCharacterId : -1);
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(7, 12, 7, 12));
        row.setStyle(sel
            ? "-fx-background-color:#252b40;-fx-background-radius:6;-fx-border-color:transparent transparent transparent #5b7cf6;-fx-border-width:0 0 0 3;-fx-cursor:hand;"
            : "-fx-background-radius:6;-fx-cursor:hand;");
        Label icon = new Label("👤"); icon.setStyle("-fx-font-size:11px;");
        Label name = new Label(character.getName());
        name.setStyle("-fx-text-fill:" + (sel ? "#a0b4fc" : "#c8cfe8") + ";-fx-font-size:13px;" + (sel ? "-fx-font-weight:700;" : ""));
        HBox.setHgrow(name, Priority.ALWAYS);
        row.getChildren().addAll(icon, name);
        row.setOnMouseEntered(e -> { if (!sel) row.setStyle("-fx-background-color:#20253a;-fx-background-radius:6;-fx-cursor:hand;"); });
        row.setOnMouseExited(e -> { if (!sel) row.setStyle("-fx-background-radius:6;-fx-cursor:hand;"); });
        row.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY) onCharacterRowClick(character); });
        return row;
    }

    private void onStoryRowClick(Story story) {
        selectedStoryId = story.getId(); loadStoryIntoForm(story); showStoryEditor(); renderStoryList();
    }
    private void onCharacterRowClick(Character character) {
        selectedCharacterId = character.getId(); loadCharacterIntoForm(character); showCharacterEditor(); renderCharacterList();
    }

    // ═══════ GROUP ACTIONS ═══════════════════════════════════════════════

    @FXML private void onCreateStoryGroupClick()     { createGroup("story"); }
    @FXML private void onCreateCharacterGroupClick() { createGroup("character"); }

    private void createGroup(String type) {
        if (selectedProjectId == null) { showError("Select a project first."); return; }
        TextInputDialog d = new TextInputDialog("New Group");
        d.setTitle("Create Group"); d.setHeaderText("Group name:"); d.setContentText("Name:");
        Optional<String> r = d.showAndWait();
        if (r.isEmpty() || r.get().trim().isEmpty()) return;
        Group g = new Group(); g.setProjectId(selectedProjectId); g.setName(r.get().trim());
        g.setType(type); g.setColor(pickNextColor(type));
        Group created = service.createGroup(g);
        refreshGroups();
        openMemberPicker(created, type, () -> { refreshGroups(); if ("story".equals(type)) { renderStoryList(); renderGraph(); } else { renderCharacterList(); renderGraph(); } });
    }

    private void onStoryGroupRename(Group g)      { renameGroup(g, "story"); }
    private void onCharacterGroupRename(Group g)  { renameGroup(g, "character"); }
    private void renameGroup(Group g, String type) {
        TextInputDialog d = new TextInputDialog(g.getName());
        d.setTitle("Rename Group"); d.setHeaderText("New name:"); d.setContentText("Name:");
        Optional<String> r = d.showAndWait();
        if (r.isEmpty() || r.get().trim().isEmpty()) return;
        g.setName(r.get().trim()); service.updateGroup(g); refreshGroups();
        if ("story".equals(type)) renderStoryList(); else renderCharacterList();
    }

    private void onStoryGroupDelete(Group g)      { deleteGroup(g, "story"); }
    private void onCharacterGroupDelete(Group g)  { deleteGroup(g, "character"); }
    private void deleteGroup(Group g, String type) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete Group"); a.setHeaderText("Delete \"" + g.getName() + "\"?");
        a.setContentText("Items will be ungrouped.");
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;
        service.deleteGroup(g.getId()); refreshGroups();
        if ("story".equals(type)) { renderStoryList(); renderGraph(); } else { renderCharacterList(); renderGraph(); }
    }

    private void onStoryGroupRecolor(Group g)     { recolorGroup(g, "story"); }
    private void onCharacterGroupRecolor(Group g) { recolorGroup(g, "character"); }
    private void recolorGroup(Group g, String type) {
        Stage dlg = new Stage(); dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Pick color"); dlg.setResizable(false);
        Label title = new Label("Choose group color");
        title.setStyle("-fx-text-fill:#d0d4e8;-fx-font-size:13px;-fx-font-weight:600;");
        HBox palette = new HBox(8); palette.setAlignment(Pos.CENTER); palette.setPadding(new Insets(8));
        for (String hex : GROUP_COLORS) {
            Button sw = new Button(); sw.setPrefSize(30, 30);
            sw.setStyle("-fx-background-color:" + hex + ";-fx-background-radius:15;-fx-border-color:" + (hex.equals(g.getColor()) ? "#fff" : "transparent") + ";-fx-border-radius:15;-fx-border-width:2;-fx-cursor:hand;");
            sw.setOnAction(e -> { g.setColor(hex); service.updateGroup(g); refreshGroups(); if ("story".equals(type)) { renderStoryList(); renderGraph(); } else { renderCharacterList(); renderGraph(); } dlg.close(); });
            palette.getChildren().add(sw);
        }
        VBox root = new VBox(12, title, palette); root.setPadding(new Insets(20)); root.setStyle("-fx-background-color:#1c1f2a;"); root.setAlignment(Pos.CENTER);
        dlg.setScene(new Scene(root)); dlg.showAndWait();
    }

    private void onManageStoryGroupMembers(Group g)     { openMemberPicker(g, "story", () -> { refreshGroups(); renderStoryList(); renderGraph(); }); }
    private void onManageCharacterGroupMembers(Group g) { openMemberPicker(g, "character", () -> { refreshGroups(); renderCharacterList(); renderGraph(); }); }

    private void openMemberPicker(Group group, String type, Runnable onDone) {
        Stage dlg = new Stage(); dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Members of \"" + group.getName() + "\""); dlg.setMinWidth(380); dlg.setMinHeight(400);
        Label header = new Label("Select members:");
        header.setStyle("-fx-text-fill:#c8cfe8;-fx-font-size:13px;");
        VBox checkList = new VBox(4); checkList.setPadding(new Insets(4));
        List<CheckBox> cbs = new ArrayList<>();
        if ("story".equals(type)) {
            for (Story s : stories) {
                CheckBox cb = new CheckBox(s.getTitle()); cb.setSelected(group.getMemberIds().contains(s.getId())); cb.setUserData(s.getId());
                cb.setStyle("-fx-text-fill:#c8cfe8;-fx-font-size:13px;"); cbs.add(cb); checkList.getChildren().add(cb);
            }
        } else {
            for (Character c : characters) {
                CheckBox cb = new CheckBox(c.getName()); cb.setSelected(group.getMemberIds().contains(c.getId())); cb.setUserData(c.getId());
                cb.setStyle("-fx-text-fill:#c8cfe8;-fx-font-size:13px;"); cbs.add(cb); checkList.getChildren().add(cb);
            }
        }
        ScrollPane scroll = new ScrollPane(checkList); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#151821;-fx-background:#151821;-fx-border-color:#2b3142;-fx-border-radius:7;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color:#5b7cf6;-fx-text-fill:#fff;-fx-font-weight:600;-fx-background-radius:7;-fx-padding:7 20 7 20;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#8e95a8;-fx-background-radius:7;-fx-padding:7 16 7 16;");
        saveBtn.setOnAction(e -> {
            List<Integer> sel = cbs.stream().filter(CheckBox::isSelected).map(cb -> (Integer) cb.getUserData()).collect(Collectors.toList());
            service.setGroupMembers(group.getId(), sel);
            Group reloaded = service.getGroup(group.getId());
            if (reloaded != null) { group.getMemberIds().clear(); group.getMemberIds().addAll(reloaded.getMemberIds()); }
            dlg.close(); onDone.run();
        });
        cancelBtn.setOnAction(e -> dlg.close());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox footer = new HBox(8, sp, cancelBtn, saveBtn); footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 12, 10, 12));
        footer.setStyle("-fx-background-color:#1c1f2a;-fx-border-color:#2b3142 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        VBox root = new VBox(12, header, scroll, footer); root.setPadding(new Insets(16, 16, 0, 16)); root.setStyle("-fx-background-color:#181a1f;");
        Scene scene = new Scene(root, 380, 440);
        scene.getStylesheets().add(com.inkwell.app.InkwellApplication.class.getResource("/styles/theme.css").toExternalForm());
        dlg.setScene(scene); dlg.showAndWait();
    }

    private void refreshGroups() {
        storyGroups.clear(); characterGroups.clear();
        if (selectedProjectId == null) return;
        storyGroups.addAll(service.getGroupsByProject(selectedProjectId, "story"));
        characterGroups.addAll(service.getGroupsByProject(selectedProjectId, "character"));
    }

    private String pickNextColor(String type) {
        List<Group> existing = "story".equals(type) ? storyGroups : characterGroups;
        Set<String> used = existing.stream().map(Group::getColor).collect(Collectors.toSet());
        for (String c : GROUP_COLORS) if (!used.contains(c)) return c;
        return GROUP_COLORS[existing.size() % GROUP_COLORS.length];
    }

    private Color parseColor(String hex) { try { return Color.web(hex); } catch (Exception e) { return Color.web("#5b7cf6"); } }

    // ═══════ RICH EDITORS ════════════════════════════════════════════════

    private void buildStoryContentEditor() {
        storyContentEditor = new HTMLEditor(); styleHtmlEditor(storyContentEditor); VBox.setVgrow(storyContentEditor, Priority.ALWAYS);
        Button ex = makeIconButton("⤢", "Expand", this::toggleStoryContentExpand);
        Button fs = makeIconButton("⛶", "Fullscreen", () -> openFullscreenEditor("Story Content", storyContentEditor.getHtmlText(), html -> storyContentEditor.setHtmlText(html)));
        storyContentEditorContainer.getChildren().setAll(editorHeader("CONTENT", ex, fs), storyContentEditor);
    }

    private void buildCharacterDescEditor() {
        characterDescriptionEditor = new HTMLEditor(); styleHtmlEditor(characterDescriptionEditor); VBox.setVgrow(characterDescriptionEditor, Priority.ALWAYS);
        Button ex = makeIconButton("⤢", "Expand", this::toggleCharacterContentExpand);
        Button fs = makeIconButton("⛶", "Fullscreen", () -> openFullscreenEditor("Character Description", characterDescriptionEditor.getHtmlText(), html -> characterDescriptionEditor.setHtmlText(html)));
        characterDescEditorContainer.getChildren().setAll(editorHeader("DESCRIPTION", ex, fs), characterDescriptionEditor);
    }

    private void toggleStoryContentExpand() {
        storyContentExpanded = !storyContentExpanded; setPanel(storyMetaFields, !storyContentExpanded);
        ((Button)((HBox)storyContentEditorContainer.getChildren().get(0)).getChildren().get(1)).setText(storyContentExpanded ? "⤡" : "⤢");
    }
    private void toggleCharacterContentExpand() {
        characterContentExpanded = !characterContentExpanded; setPanel(characterMetaFields, !characterContentExpanded);
        ((Button)((HBox)characterDescEditorContainer.getChildren().get(0)).getChildren().get(1)).setText(characterContentExpanded ? "⤡" : "⤢");
    }

    private void styleHtmlEditor(HTMLEditor e) {
        e.setStyle("-fx-background-color:#151821;-fx-border-color:#2b3142;-fx-border-radius:7;-fx-background-radius:7;");
        e.sceneProperty().addListener((obs, o, ns) -> { if (ns != null) applyHtmlEditorDarkTheme(e); });
    }

    private void applyHtmlEditorDarkTheme(HTMLEditor editor) {
        javafx.scene.web.WebView wv = (javafx.scene.web.WebView) editor.lookup(".web-view");
        if (wv == null) return;
        wv.getEngine().documentProperty().addListener((obs, o, nd) -> {
            if (nd == null) return;
            try { wv.getEngine().executeScript("document.body.style.backgroundColor='#151821';document.body.style.color='#e8eaf1';document.body.style.fontFamily='Segoe UI,Inter,sans-serif';document.body.style.fontSize='13px';document.body.style.margin='8px';"); } catch (Exception ignored) {}
        });
    }

    private HBox editorHeader(String t, Button... actions) {
        Label lbl = new Label(t); lbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#5a6278;-fx-padding:6 0 2 0;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox box = new HBox(6); box.setAlignment(Pos.CENTER_LEFT); box.getChildren().add(lbl); box.getChildren().add(sp);
        for (Button b : actions) box.getChildren().add(b);
        return box;
    }

    private Button makeIconButton(String icon, String tip, Runnable action) {
        Button btn = new Button(icon); btn.setTooltip(new Tooltip(tip));
        String base = "-fx-background-color:transparent;-fx-text-fill:#8e95a8;-fx-font-size:13px;-fx-padding:2 7 2 7;-fx-cursor:hand;-fx-background-radius:5;";
        String hover = "-fx-background-color:#1e2130;-fx-text-fill:#c8cfe8;-fx-font-size:13px;-fx-padding:2 7 2 7;-fx-cursor:hand;-fx-background-radius:5;";
        btn.setStyle(base); btn.setOnMouseEntered(e -> btn.setStyle(hover)); btn.setOnMouseExited(e -> btn.setStyle(base)); btn.setOnAction(e -> action.run());
        return btn;
    }

    private void openFullscreenEditor(String title, String html, Consumer<String> onSave) {
        Stage dlg = new Stage(); dlg.initModality(Modality.APPLICATION_MODAL); dlg.setTitle(title); dlg.setMinWidth(760); dlg.setMinHeight(560);

        // Fill primary screen
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        dlg.setX(screen.getMinX()); dlg.setY(screen.getMinY());
        dlg.setWidth(screen.getWidth()); dlg.setHeight(screen.getHeight());
        HTMLEditor editor = new HTMLEditor(); editor.setHtmlText(html); styleHtmlEditor(editor); VBox.setVgrow(editor, Priority.ALWAYS);
        Button save = new Button("✓ Save & Close"); save.setStyle("-fx-background-color:#5b7cf6;-fx-text-fill:#fff;-fx-font-weight:600;-fx-background-radius:7;-fx-padding:8 20 8 20;");
        Button cancel = new Button("Cancel"); cancel.setStyle("-fx-background-color:transparent;-fx-text-fill:#8e95a8;-fx-background-radius:7;-fx-padding:8 16 8 16;");
        save.setOnAction(e -> { onSave.accept(editor.getHtmlText()); dlg.close(); }); cancel.setOnAction(e -> dlg.close());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox footer = new HBox(8, sp, cancel, save); footer.setAlignment(Pos.CENTER_RIGHT); footer.setPadding(new Insets(10, 16, 10, 16));
        footer.setStyle("-fx-background-color:#1c1f2a;-fx-border-color:#2b3142 transparent transparent transparent;-fx-border-width:1 0 0 0;");
        VBox root = new VBox(editor, footer); root.setStyle("-fx-background-color:#151821;");
        Scene scene = new Scene(root, screen.getWidth(), screen.getHeight()); scene.getStylesheets().add(com.inkwell.app.InkwellApplication.class.getResource("/styles/theme.css").toExternalForm());
        dlg.setScene(scene); dlg.showAndWait();
    }

    // ═══════ NAV ═════════════════════════════════════════════════════════

    @FXML private void onStoriesNavClick()    { showStoriesView(); }
    @FXML private void onCharactersNavClick() { showCharactersView(); }
    @FXML private void onGraphNavClick()      { showGraphView(); }

    // ═══════ EDITOR PANEL ════════════════════════════════════════════════

    private void showStoryEditor() {
        storyEditorExpanded = false; storyExpandButton.setText("⤢ Expand");
        setPanel(storyEditorPanel, true); setPanel(storiesListPanel, true);
        HBox.setHgrow(storiesListPanel, Priority.ALWAYS); HBox.setHgrow(storyEditorPanel, Priority.NEVER);
    }
    private void hideStoryEditor() {
        storyEditorExpanded = false; storyExpandButton.setText("⤢ Expand");
        setPanel(storyEditorPanel, false); setPanel(storiesListPanel, true);
        HBox.setHgrow(storiesListPanel, Priority.ALWAYS);
        if (storyContentExpanded) { storyContentExpanded = false; setPanel(storyMetaFields, true); }
        clearStoryForm();
    }
    @FXML private void onStoryExpandClick() {
        storyEditorExpanded = !storyEditorExpanded;
        if (storyEditorExpanded) { storyExpandButton.setText("⤡ Collapse"); setPanel(storiesListPanel, false); HBox.setHgrow(storyEditorPanel, Priority.ALWAYS); }
        else { storyExpandButton.setText("⤢ Expand"); setPanel(storiesListPanel, true); HBox.setHgrow(storiesListPanel, Priority.ALWAYS); HBox.setHgrow(storyEditorPanel, Priority.NEVER); }
    }
    @FXML private void onStoryBackClick() { hideStoryEditor(); selectedStoryId = null; renderStoryList(); }

    private void showCharacterEditor() {
        characterEditorExpanded = false; characterExpandButton.setText("⤢ Expand");
        setPanel(characterEditorPanel, true); setPanel(charactersListPanel, true);
        HBox.setHgrow(charactersListPanel, Priority.ALWAYS); HBox.setHgrow(characterEditorPanel, Priority.NEVER);
    }
    private void hideCharacterEditor() {
        characterEditorExpanded = false; characterExpandButton.setText("⤢ Expand");
        setPanel(characterEditorPanel, false); setPanel(charactersListPanel, true);
        HBox.setHgrow(charactersListPanel, Priority.ALWAYS);
        if (characterContentExpanded) { characterContentExpanded = false; setPanel(characterMetaFields, true); }
        clearCharacterForm();
    }
    @FXML private void onCharacterExpandClick() {
        characterEditorExpanded = !characterEditorExpanded;
        if (characterEditorExpanded) { characterExpandButton.setText("⤡ Collapse"); setPanel(charactersListPanel, false); HBox.setHgrow(characterEditorPanel, Priority.ALWAYS); }
        else { characterExpandButton.setText("⤢ Expand"); setPanel(charactersListPanel, true); HBox.setHgrow(charactersListPanel, Priority.ALWAYS); HBox.setHgrow(characterEditorPanel, Priority.NEVER); }
    }
    @FXML private void onCharacterBackClick() { hideCharacterEditor(); selectedCharacterId = null; renderCharacterList(); }

    private void setPanel(VBox p, boolean v) { p.setVisible(v); p.setManaged(v); }

    // ═══════ PROJECT ═════════════════════════════════════════════════════

    @FXML private void onCreateProjectClick() {
        TextInputDialog d = new TextInputDialog(); d.setTitle("Create Project"); d.setHeaderText("New project"); d.setContentText("Name:");
        Optional<String> r = d.showAndWait();
        if (r.isEmpty() || r.get().trim().isEmpty()) return;
        Project p = new Project(); p.setName(r.get().trim()); p.setDescription("Workspace project");
        try { Project created = service.createProject(p); loadProjects(); selectProjectById(created.getId()); }
        catch (Exception e) { showError("Failed to create project: " + e.getMessage()); }
    }
    @FXML private void onDeleteProjectClick() {
        ProjectOption sel = projectComboBox.getValue(); if (sel == null) return;
        if (projects.size() <= 1) { showError("At least one project must remain."); return; }
        try { service.deleteProject(sel.id()); loadProjects(); }
        catch (Exception e) { showError("Failed to delete project: " + e.getMessage()); }
    }

    // ═══════ STORIES ═════════════════════════════════════════════════════

    @FXML private void onStorySearchClick() { applyStoryFilters(); }
    @FXML private void onStoryClearFiltersClick() { storySearchField.clear(); tagFilterComboBox.setValue(ALL_TAGS_OPTION); applyStoryFilters(); }
    @FXML private void onNewStoryClick() { clearStoryForm(); selectedStoryId = null; showStoryEditor(); renderStoryList(); }

    @FXML private void onSaveStoryClick() {
        if (selectedProjectId == null) { showError("Select a project first."); return; }
        String title = storyTitleField.getText() == null ? "" : storyTitleField.getText().trim();
        if (title.isEmpty()) { showError("Story title is required."); return; }
        Story s = new Story(); if (selectedStoryId != null) s.setId(selectedStoryId);
        s.setProjectId(selectedProjectId); s.setTitle(title); s.setCategory(valueOrNull(storyCategoryField.getText()));
        s.setSummary(valueOrNull(storySummaryArea.getText())); s.setContent(valueOrNull(storyContentEditor.getHtmlText()));
        try {
            Story persisted = selectedStoryId == null ? service.createStory(s) : s;
            if (selectedStoryId != null) { service.updateStory(s); persisted = service.getStory(s.getId()); }
            service.setStoryTags(persisted.getId(), parseTags(storyTagsField.getText()));
            selectedStoryId = persisted.getId(); refreshStories(); loadTagOptions(); loadStoryIntoForm(persisted); showStoryEditor(); renderGraph();
        } catch (Exception e) { showError("Failed to save story: " + e.getMessage()); }
    }

    @FXML private void onDeleteStoryClick() {
        if (selectedStoryId == null) return;
        try { service.deleteStory(selectedStoryId); selectedStoryId = null; refreshStories(); loadTagOptions(); hideStoryEditor(); refreshCharacters(); refreshStoryOptionsForCharacterForm(); renderGraph(); }
        catch (Exception e) { showError("Failed to delete story: " + e.getMessage()); }
    }

    // ═══════ CHARACTERS ══════════════════════════════════════════════════

    @FXML private void onNewCharacterClick() { clearCharacterForm(); selectedCharacterId = null; showCharacterEditor(); renderCharacterList(); }

    @FXML private void onSaveCharacterClick() {
        if (selectedProjectId == null) { showError("Select a project first."); return; }
        String name = characterNameField.getText() == null ? "" : characterNameField.getText().trim();
        if (name.isEmpty()) { showError("Character name is required."); return; }
        Character ch = new Character(); if (selectedCharacterId != null) ch.setId(selectedCharacterId);
        ch.setProjectId(selectedProjectId); ch.setName(name); ch.setDescription(valueOrNull(characterDescriptionEditor.getHtmlText()));
        ch.setTraits(valueOrNull(characterTraitsField.getText()));
        StoryOption ss = characterStoryComboBox.getValue(); ch.setStoryId(ss == null ? null : ss.id());
        try {
            Character persisted = selectedCharacterId == null ? service.createCharacter(ch) : ch;
            if (selectedCharacterId != null) { service.updateCharacter(ch); persisted = service.getCharacter(ch.getId()); }
            selectedCharacterId = persisted.getId(); refreshCharacters(); loadCharacterIntoForm(persisted); showCharacterEditor(); renderGraph();
        } catch (Exception e) { showError("Failed to save character: " + e.getMessage()); }
    }

    @FXML private void onDeleteCharacterClick() {
        if (selectedCharacterId == null) return;
        try { service.deleteCharacter(selectedCharacterId); selectedCharacterId = null; refreshCharacters(); hideCharacterEditor(); renderGraph(); }
        catch (Exception e) { showError("Failed to delete character: " + e.getMessage()); }
    }

    @FXML private void onGraphRefreshClick() { renderGraph(); }

    // ═══════ PROJECT SELECTOR ════════════════════════════════════════════

    private void setupProjectSelector() {
        projectComboBox.setItems(projects);
        projectComboBox.valueProperty().addListener((obs, o, nv) -> {
            if (nv == null) return; selectedProjectId = nv.id();
            hideStoryEditor(); hideCharacterEditor(); selectedStoryId = null; selectedCharacterId = null;
            loadTagOptions(); refreshGroups(); refreshStories(); refreshCharacters();
        });
    }

    private void loadProjects() {
        try {
            List<ProjectOption> opts = service.getProjects().stream().map(p -> new ProjectOption(p.getId(), p.getName())).toList();
            projects.setAll(opts);
            if (!opts.isEmpty()) { ProjectOption cur = projectComboBox.getValue(); if (cur != null) selectProjectById(cur.id()); else projectComboBox.setValue(opts.get(0)); }
        } catch (Exception e) { showError("Failed to load projects: " + e.getMessage()); }
    }

    private void selectProjectById(int id) {
        for (ProjectOption o : projects) if (o.id() == id) { projectComboBox.setValue(o); return; }
        if (!projects.isEmpty()) projectComboBox.setValue(projects.get(0));
    }

    // ═══════ DATA REFRESH ════════════════════════════════════════════════

    private void refreshStories() {
        if (selectedProjectId == null) { stories.clear(); renderStoryList(); return; }
        try { stories.setAll(service.getStoriesByProject(selectedProjectId)); refreshStoryOptionsForCharacterForm(); renderStoryList(); renderGraph(); }
        catch (Exception e) { showError("Failed to load stories: " + e.getMessage()); }
    }

    private void refreshCharacters() {
        if (selectedProjectId == null) { characters.clear(); renderCharacterList(); return; }
        try { characters.setAll(service.getCharactersByProject(selectedProjectId)); renderCharacterList(); renderGraph(); }
        catch (Exception e) { showError("Failed to load characters: " + e.getMessage()); }
    }

    private void applyStoryFilters() {
        if (selectedProjectId == null) { stories.clear(); renderStoryList(); return; }
        try {
            List<Story> base = new ArrayList<>(service.getStoriesByProject(selectedProjectId));
            String tag = tagFilterComboBox.getValue();
            if (tag != null && !ALL_TAGS_OPTION.equals(tag)) { Set<Integer> ids = service.filterStoriesByTag(tag).stream().map(Story::getId).collect(Collectors.toSet()); base = base.stream().filter(s -> ids.contains(s.getId())).toList(); }
            String q = storySearchField.getText();
            if (q != null && !q.trim().isEmpty()) { String n = q.trim().toLowerCase(); base = base.stream().filter(s -> contains(s.getTitle(), n) || contains(s.getSummary(), n) || contains(s.getContent(), n)).collect(Collectors.toList()); }
            stories.setAll(base); renderStoryList();
        } catch (Exception e) { showError("Failed to filter stories: " + e.getMessage()); }
    }

    private boolean contains(String v, String q) { return v != null && v.toLowerCase().contains(q); }

    private void loadTagOptions() {
        if (selectedProjectId == null) { tagFilterComboBox.setItems(FXCollections.observableArrayList(List.of(ALL_TAGS_OPTION))); tagFilterComboBox.setValue(ALL_TAGS_OPTION); return; }
        try {
            List<String> vals = new ArrayList<>(); vals.add(ALL_TAGS_OPTION);
            List<Story> ps = service.getStoriesByProject(selectedProjectId); Set<Integer> psIds = ps.stream().map(Story::getId).collect(Collectors.toSet());
            for (Tag t : service.getTags()) { if (service.filterStoriesByTag(t.getName()).stream().anyMatch(s -> psIds.contains(s.getId()))) vals.add(t.getName()); }
            tagFilterComboBox.setItems(FXCollections.observableArrayList(vals)); if (!vals.isEmpty()) tagFilterComboBox.setValue(vals.get(0));
        } catch (Exception e) { showError("Failed to load tags: " + e.getMessage()); }
    }

    private void refreshStoryOptionsForCharacterForm() {
        List<StoryOption> opts = new ArrayList<>(); opts.add(NO_STORY_OPTION);
        if (selectedProjectId != null) for (Story s : service.getStoriesByProject(selectedProjectId)) opts.add(new StoryOption(s.getId(), s.getTitle()));
        characterStoryComboBox.setItems(FXCollections.observableArrayList(opts));
        if (characterStoryComboBox.getValue() == null) characterStoryComboBox.setValue(NO_STORY_OPTION);
    }

    private void loadStoryIntoForm(Story story) {
        selectedStoryId = story.getId(); storyTitleField.setText(story.getTitle()); storyCategoryField.setText(story.getCategory()); storySummaryArea.setText(story.getSummary());
        String content = story.getContent(); storyContentEditor.setHtmlText(content != null ? content : "");
        storyTagsField.setText(service.getStoryTags(story.getId()).stream().map(Tag::getName).collect(Collectors.joining(", ")));
    }

    private void loadCharacterIntoForm(Character character) {
        selectedCharacterId = character.getId(); characterNameField.setText(character.getName());
        String desc = character.getDescription(); characterDescriptionEditor.setHtmlText(desc != null ? desc : "");
        characterTraitsField.setText(character.getTraits());
        Integer storyId = character.getStoryId();
        if (storyId == null) { characterStoryComboBox.setValue(NO_STORY_OPTION); return; }
        for (StoryOption o : characterStoryComboBox.getItems()) if (storyId.equals(o.id())) { characterStoryComboBox.setValue(o); return; }
        characterStoryComboBox.setValue(NO_STORY_OPTION);
    }

    private void clearStoryForm() {
        storyTitleField.clear(); storyCategoryField.clear(); storySummaryArea.clear(); storyTagsField.clear();
        if (storyContentEditor != null) storyContentEditor.setHtmlText("");
    }

    private void clearCharacterForm() {
        characterNameField.clear(); characterTraitsField.clear();
        if (characterDescriptionEditor != null) characterDescriptionEditor.setHtmlText("");
        if (characterStoryComboBox.getItems() == null || characterStoryComboBox.getItems().isEmpty()) refreshStoryOptionsForCharacterForm();
        characterStoryComboBox.setValue(NO_STORY_OPTION);
    }

    // ═══════ VIEWS ═══════════════════════════════════════════════════════

    private void showStoriesView() {
        contentTitleLabel.setText("Stories"); setView(storiesView, true); setView(charactersView, false); setView(graphView, false);
        setNavState(storiesNavButton, true); setNavState(charactersNavButton, false); setNavState(graphNavButton, false);
    }
    private void showCharactersView() {
        contentTitleLabel.setText("Characters"); setView(storiesView, false); setView(charactersView, true); setView(graphView, false);
        setNavState(storiesNavButton, false); setNavState(charactersNavButton, true); setNavState(graphNavButton, false);
    }
    private void showGraphView() {
        contentTitleLabel.setText("Graph"); setView(storiesView, false); setView(charactersView, false); setView(graphView, true);
        setNavState(storiesNavButton, false); setNavState(charactersNavButton, false); setNavState(graphNavButton, true); renderGraph();
    }

    private void setView(VBox view, boolean v) { view.setVisible(v); view.setManaged(v); }
    private void setNavState(Button b, boolean active) {
        if (active) { if (!b.getStyleClass().contains("nav-active")) b.getStyleClass().add("nav-active"); } else b.getStyleClass().remove("nav-active");
    }
    private void showError(String msg) { Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText("Inkwell"); a.setContentText(msg); a.showAndWait(); }

    // ═══════ GRAPH ═══════════════════════════════════════════════════════

    private void setupGraphInteractions() {
        graphPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            double f = event.getDeltaY() > 0 ? 1.08 : 0.92;
            graphScale = clamp(graphScale * f, 0.4, 2.5); graphPane.setScaleX(graphScale); graphPane.setScaleY(graphScale); event.consume();
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GRAPH — group-centric: each group is a draggable Pane container
    //  Nodes belonging to a group live INSIDE that Pane (local coordinates).
    //  A node that appears in multiple groups gets a SEPARATE visual copy per group.
    //  Ungrouped nodes sit directly on graphPane.
    // ═══════════════════════════════════════════════════════════════════════

    private void renderGraph() {
        if (graphPane == null) return;
        graphPane.getChildren().clear();
        graphNodeLookup.clear();
        if (selectedProjectId == null) return;

        List<Story>        allStories    = service.getStoriesByProject(selectedProjectId);
        List<Character>    allCharacters = service.getCharactersByProject(selectedProjectId);
        List<Relationship> relationships = service.getRelationships();

        // Track which story/character ids are covered by at least one group
        Set<Integer> groupedStoryIds     = storyGroups.stream().flatMap(g -> g.getMemberIds().stream()).collect(Collectors.toSet());
        Set<Integer> groupedCharacterIds = characterGroups.stream().flatMap(g -> g.getMemberIds().stream()).collect(Collectors.toSet());

        // -- 1. Build draggable group containers ---------------------------------
        // Layout: story groups column ~160, character groups column ~820
        double groupColStory = 160, groupColChar = 820, groupStartY = 80, groupRowStep = 0;

        // Place story groups
        double sgY = groupStartY;
        for (Group g : storyGroups) {
            Pane container = buildGroupContainer(g, "story", allStories, sgY);
            if (container != null) {
                graphPane.getChildren().add(container);
                sgY += container.getPrefHeight() + 30;
            }
        }

        // Place character groups
        double cgY = groupStartY;
        for (Group g : characterGroups) {
            Pane container = buildGroupContainer(g, "character", allCharacters, cgY);
            if (container != null) {
                graphPane.getChildren().add(container);
                cgY += container.getPrefHeight() + 30;
            }
        }

        // -- 2. Collect ungrouped nodes (don't add to pane yet) ------------------
        List<Node> ungroupedNodes = new ArrayList<>();

        double ux = 160, uy = Math.max(sgY + 20, groupStartY);
        for (Story st : allStories) {
            if (groupedStoryIds.contains(st.getId())) continue;
            GraphNode gn = createFreeNode("story-" + st.getId(), st.getTitle(), "Story",
                    ux, uy, "#415a77", () -> openStoryFromGraph(st.getId()), graphPane);
            graphNodeLookup.put("story-" + st.getId(), gn);
            ungroupedNodes.add(gn.view());
            uy += 110;
        }

        double cx = 820, cy = Math.max(cgY + 20, groupStartY);
        for (Character ch : allCharacters) {
            if (groupedCharacterIds.contains(ch.getId())) continue;
            GraphNode gn = createFreeNode("character-" + ch.getId(), ch.getName(), "Character",
                    cx, cy, "#6d597a", () -> openCharacterFromGraph(ch.getId()), graphPane);
            graphNodeLookup.put("character-" + ch.getId(), gn);
            ungroupedNodes.add(gn.view());
            cy += 110;
        }

        // -- 3. Edges between ungrouped nodes ------------------------------------
        List<Line> edges = new ArrayList<>();
        for (Character ch : allCharacters) {
            if (ch.getStoryId() == null) continue;
            GraphNode src = graphNodeLookup.get("character-" + ch.getId());
            GraphNode tgt = graphNodeLookup.get("story-" + ch.getStoryId());
            if (src != null && tgt != null) edges.add(createEdge(src, tgt, Color.web("#7f8ea3")));
        }
        for (Relationship rel : relationships) {
            GraphNode src = rel.getSourceCharacterId() == null ? null : graphNodeLookup.get("character-" + rel.getSourceCharacterId());
            GraphNode tgt = rel.getTargetCharacterId() == null ? null : graphNodeLookup.get("character-" + rel.getTargetCharacterId());
            if (src != null && tgt != null) edges.add(createEdge(src, tgt, Color.web("#b57edc")));
        }

        // Z-order: group containers → edges → ungrouped nodes (nodes always on top)
        graphPane.getChildren().addAll(edges);
        graphPane.getChildren().addAll(ungroupedNodes);
    }

    /**
     * Builds a self-contained draggable Pane that acts as a group container.
     * Each member gets its OWN node copy inside this Pane (local coords).
     * This means a node in 2 groups = 2 separate visual copies → no merging.
     */
    @SuppressWarnings("unchecked")
    private <T> Pane buildGroupContainer(Group group, String type,
                                          List<T> allItems, double startY) {
        List<T> members = allItems.stream()
                .filter(item -> {
                    int id = "story".equals(type)
                            ? ((Story) item).getId()
                            : ((Character) item).getId();
                    return group.getMemberIds().contains(id);
                })
                .collect(Collectors.toList());
        if (members.isEmpty()) return null;

        Color gc = parseColor(group.getColor());
        String colorHex = group.getColor();
        String nodeColor = "story".equals(type) ? "#415a77" : "#6d597a";

        // Layout nodes inside container (vertical stack)
        double pad = 22, nodeW = 160, nodeH = 64, spacing = 14;
        double labelH = 28;
        double innerW = nodeW + pad * 2;
        double innerH = labelH + pad + members.size() * (nodeH + spacing) - spacing + pad;

        // Container pane
        Pane container = new Pane();
        container.setPrefSize(innerW, innerH);
        container.setLayoutX("story".equals(type) ? 140 : 820);
        container.setLayoutY(startY);

        // Background rectangle
        Rectangle bg = new Rectangle(0, 0, innerW, innerH);
        bg.setArcWidth(16); bg.setArcHeight(16);
        bg.setFill(gc.deriveColor(0, 1, 1, 0.10));
        bg.setStroke(gc.deriveColor(0, 1, 1, 0.65));
        bg.setStrokeWidth(1.5);
        bg.setMouseTransparent(true);
        container.getChildren().add(bg);

        // Group label
        Label nameLbl = new Label(group.getName());
        nameLbl.setLayoutX(10);
        nameLbl.setLayoutY(6);
        nameLbl.setStyle(
            "-fx-text-fill:" + colorHex + ";" +
            "-fx-font-size:11px;-fx-font-weight:700;"
        );
        nameLbl.setMouseTransparent(true);
        container.getChildren().add(nameLbl);

        // Member nodes — each is an INDEPENDENT copy inside this container
        double ny = labelH + pad * 0.5;
        for (T item : members) {
            int itemId; String itemTitle;
            Runnable clickAction;
            if ("story".equals(type)) {
                Story st = (Story) item;
                itemId = st.getId(); itemTitle = st.getTitle();
                clickAction = () -> openStoryFromGraph(st.getId());
            } else {
                Character ch = (Character) item;
                itemId = ch.getId(); itemTitle = ch.getName();
                clickAction = () -> openCharacterFromGraph(ch.getId());
            }

            // Unique key per group+member to avoid collisions in lookup
            String nodeKey = "g" + group.getId() + "-" + type + "-" + itemId;
            GraphNode gn = createFreeNode(nodeKey, itemTitle, cap(type),
                    pad, ny, nodeColor, clickAction, container);
            graphNodeLookup.put(nodeKey, gn);
            container.getChildren().add(gn.view());
            ny += nodeH + spacing;
        }

        // Draggable: entire container moves together
        makeContainerDraggable(container);
        return container;
    }

    /** Capitalises first letter */
    private String cap(String s) { return s.isEmpty() ? s : java.lang.Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /**
     * Creates a node whose drag is confined to its parent Pane
     * (works for both graphPane and group containers).
     */
    private GraphNode createFreeNode(String key, String title, String type,
                                      double x, double y, String color,
                                      Runnable onClick, Pane parent) {
        StackPane node = new StackPane();
        node.setPrefSize(160, 64);
        node.setLayoutX(x); node.setLayoutY(y);
        node.setAlignment(Pos.CENTER); node.setCursor(Cursor.HAND);

        Rectangle card = new Rectangle(160, 64);
        card.setArcHeight(14); card.setArcWidth(14);
        card.setFill(Color.web(color));
        card.setStroke(Color.web("#dbe4ff")); card.setStrokeWidth(1.0);

        Label label = new Label(type + "\n" + trimForNode(title));
        label.setTextAlignment(TextAlignment.CENTER);
        label.setStyle("-fx-text-fill:#f5f6ff;-fx-font-size:12px;-fx-font-weight:600;");

        node.getChildren().addAll(card, label);
        GraphNode gn = new GraphNode(key, node);

        DragState ds = new DragState();
        node.setOnMousePressed(ev -> {
            javafx.geometry.Point2D l = parent.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            ds.offsetX = l.getX() - node.getLayoutX();
            ds.offsetY = l.getY() - node.getLayoutY();
            ds.wasDragged = false;
            ev.consume();
        });
        node.setOnMouseDragged(ev -> {
            javafx.geometry.Point2D l = parent.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            node.setLayoutX(clamp(l.getX() - ds.offsetX, 0, parent.getWidth() - node.getPrefWidth()));
            node.setLayoutY(clamp(l.getY() - ds.offsetY, 0, parent.getHeight() - node.getPrefHeight()));
            ds.wasDragged = true;
            ev.consume();
        });
        node.setOnMouseClicked(ev -> {
            if (!ds.wasDragged && ev.getButton() == MouseButton.PRIMARY) onClick.run();
            ev.consume();
        });
        return gn;
    }

    /** Makes a group container Pane draggable on graphPane. */
    private void makeContainerDraggable(Pane container) {
        DragState ds = new DragState();
        // Drag starts only when clicking the background (not on child nodes)
        container.setOnMousePressed(ev -> {
            if (ev.getTarget() == container || ev.getTarget() instanceof Rectangle
                    || ev.getTarget() instanceof Label) {
                javafx.geometry.Point2D l = graphPane.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                ds.offsetX = l.getX() - container.getLayoutX();
                ds.offsetY = l.getY() - container.getLayoutY();
                ds.wasDragged = false;
                ev.consume();
            }
        });
        container.setOnMouseDragged(ev -> {
            javafx.geometry.Point2D l = graphPane.sceneToLocal(ev.getSceneX(), ev.getSceneY());
            container.setLayoutX(clamp(l.getX() - ds.offsetX, 0, graphPane.getWidth() - container.getPrefWidth()));
            container.setLayoutY(clamp(l.getY() - ds.offsetY, 0, graphPane.getHeight() - container.getPrefHeight()));
            ds.wasDragged = true;
            ev.consume();
        });
    }

    private Line createEdge(GraphNode src, GraphNode tgt, Color color) {
        Line line = new Line(); line.setStroke(color); line.setStrokeWidth(1.8);
        line.startXProperty().bind(src.centerXProperty()); line.startYProperty().bind(src.centerYProperty());
        line.endXProperty().bind(tgt.centerXProperty()); line.endYProperty().bind(tgt.centerYProperty());
        line.setMouseTransparent(true); return line;
    }

    private void openStoryFromGraph(int storyId) {
        Story story = service.getStory(storyId); if (story == null) return;
        if (story.getProjectId() != null && !story.getProjectId().equals(selectedProjectId)) selectProjectById(story.getProjectId());
        showStoriesView(); selectedStoryId = storyId; loadStoryIntoForm(story); showStoryEditor(); renderStoryList();
    }

    private void openCharacterFromGraph(int characterId) {
        Character ch = service.getCharacter(characterId); if (ch == null) return;
        if (ch.getProjectId() != null && !ch.getProjectId().equals(selectedProjectId)) selectProjectById(ch.getProjectId());
        showCharactersView(); selectedCharacterId = characterId; loadCharacterIntoForm(ch); showCharacterEditor(); renderCharacterList();
    }

    // ═══════ HELPERS ═════════════════════════════════════════════════════

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return List.of();
        return Arrays.stream(rawTags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).distinct().toList();
    }
    private String valueOrNull(String v) { if (v == null) return null; String t = v.trim(); return t.isEmpty() ? null : t; }
    private String trimForNode(String v) { if (v == null || v.isBlank()) return "Untitled"; String t = v.trim(); return t.length() <= 26 ? t : t.substring(0, 23) + "..."; }
    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    // ═══════ INNER TYPES ═════════════════════════════════════════════════

    private record StoryOption(Integer id, String title) { @Override public String toString() { return title; } }
    private record ProjectOption(Integer id, String title) { @Override public String toString() { return title; } }
    private record GraphNode(String key, StackPane view) {
        DoubleBinding centerXProperty() { return view.layoutXProperty().add(view.widthProperty().divide(2.0)); }
        DoubleBinding centerYProperty() { return view.layoutYProperty().add(view.heightProperty().divide(2.0)); }
    }
    private static class DragState { double offsetX, offsetY; boolean wasDragged; }
}
