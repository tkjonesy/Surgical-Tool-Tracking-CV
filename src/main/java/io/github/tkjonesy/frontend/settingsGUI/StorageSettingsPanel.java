package io.github.tkjonesy.frontend.settingsGUI;

import io.github.tkjonesy.utils.Paths;
import io.github.tkjonesy.utils.settings.ProgramSettings;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;

import static io.github.tkjonesy.frontend.settingsGUI.SettingsWindow.addSettingChangeListener;

public class StorageSettingsPanel extends JPanel implements SettingsUI {

    private static final ProgramSettings settings = ProgramSettings.getCurrentSettings();
    private static final HashMap<String, Object> settingsUpdates = SettingsWindow.getSettingsUpdates();

    private final JLabel storageSelectorLabel;
    private final JLabel saveVideoLabel;
    private final JLabel saveLogsTextLabel;
    private final JLabel saveLogsCSVLabel;

    private final ButtonGroup storageSelectorGroup;

    private final JButton folderSelectorButton;
    private final JLabel selectedFolderLabel;
    private final File[] selectedFolder;
    private final JRadioButtonMenuItem defaultSaveOption;
    private final JRadioButtonMenuItem customSaveOption;
    private final JCheckBox saveVideoCheckbox;
    private final JCheckBox saveLogsTextCheckbox;
    private final JCheckBox saveLogsCSVCheckbox;

    public StorageSettingsPanel() {

        this.storageSelectorLabel = new JLabel("File Save Location");
        this.saveVideoLabel = new JLabel("Save Video");
        this.saveLogsTextLabel = new JLabel("Save Logs as Text");
        this.saveLogsCSVLabel = new JLabel("Save Logs as CSV");

        this.storageSelectorGroup = new ButtonGroup();

        this.defaultSaveOption = new JRadioButtonMenuItem("Default");
        defaultSaveOption.setToolTipText("Save to default location: " + Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);

        this.customSaveOption = new JRadioButtonMenuItem("Custom");
        customSaveOption.setToolTipText("Save to custom location");

        storageSelectorGroup.add(defaultSaveOption);
        storageSelectorGroup.add(customSaveOption);

        String settingsFileDirectory = settings.getFileDirectory();
        if(settingsFileDirectory==null)
            settingsFileDirectory = Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY;

        if(settingsFileDirectory.equals(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY))
            defaultSaveOption.setSelected(true);
        else
            customSaveOption.setSelected(true);

        selectedFolder = new File[1]; // This is so jank, I don't want to talk about it holy cow. This is the work-around for keeping this final to make the linter stfu but still make the value re-assignable
        this.folderSelectorButton = new JButton("Choose Folder...");
        this.folderSelectorButton.setToolTipText("Select a folder to save files to");
        this.selectedFolderLabel = new JLabel(settingsFileDirectory);

        folderSelectorButton.setEnabled(!defaultSaveOption.isSelected());

        this.saveVideoCheckbox = new JCheckBox();
        this.saveVideoCheckbox.setSelected(settings.isSaveVideo());

        this.saveLogsTextCheckbox = new JCheckBox();
        this.saveLogsTextCheckbox.setSelected(settings.isSaveLogsTEXT());

        this.saveLogsCSVCheckbox = new JCheckBox();
        this.saveLogsCSVCheckbox.setSelected(settings.isSaveLogsCSV());

        setLayout();
        initListeners();
    }

    @Override
    public void initListeners() {
        addSettingChangeListener(customSaveOption, (ActionListener)
                e -> {
                    folderSelectorButton.setEnabled(true);
                    settingsUpdates.put("fileDirectory", selectedFolderLabel.getText());
                    System.out.println("File directory: " + selectedFolderLabel.getText());
                    if(settings.getFileDirectory().equals(selectedFolderLabel.getText()))
                        settingsUpdates.remove("fileDirectory");
                }
        );

        addSettingChangeListener(defaultSaveOption, (ActionListener)
                e -> {
                    folderSelectorButton.setEnabled(false);
                    selectedFolderLabel.setText(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
                    settingsUpdates.put("fileDirectory", Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY);
                    if(settings.getFileDirectory().equals(Paths.DEFAULT_AIMS_SESSIONS_DIRECTORY))
                        settingsUpdates.remove("fileDirectory");
                }
        );

        addSettingChangeListener(folderSelectorButton, (ActionListener)
                e -> {
                    JFileChooser folderChooser = new JFileChooser();
                    folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    folderChooser.setAcceptAllFileFilterUsed(false);

                    int returnVal = folderChooser.showOpenDialog(StorageSettingsPanel.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        selectedFolder[0] = folderChooser.getSelectedFile();
                        selectedFolderLabel.setText(selectedFolder[0].getAbsolutePath());
                        System.out.println("Selected Folder: " + selectedFolder[0].getAbsolutePath());
                        settingsUpdates.put("fileDirectory", selectedFolder[0].getAbsolutePath());
                    }
                }
        );

        addSettingChangeListener(saveVideoCheckbox, (ActionListener)
                e -> {
                    boolean value = saveVideoCheckbox.isSelected();
                    System.out.println("Save video: " + saveVideoCheckbox.isSelected());
                    settingsUpdates.put("saveVideo", value);
                    if(settings.isSaveVideo() == value)
                        settingsUpdates.remove("saveVideo");
                }
        );
        addSettingChangeListener(saveLogsTextCheckbox, (ActionListener)
                e -> {
                    boolean value = saveLogsTextCheckbox.isSelected();
                    System.out.println("Save logs to text: " + saveLogsTextCheckbox.isSelected());
                    settingsUpdates.put("saveLogsTEXT", value);
                    if(settings.isSaveLogsTEXT() == value)
                        settingsUpdates.remove("saveLogsTEXT");
                }
        );
        addSettingChangeListener(saveLogsCSVCheckbox, (ActionListener)
                e -> {
                    boolean value = saveLogsCSVCheckbox.isSelected();
                    System.out.println("Save logs to csv: " + saveLogsCSVCheckbox.isSelected());
                    settingsUpdates.put("saveLogsCSV", value);
                    if(settings.isSaveLogsCSV() == value)
                        settingsUpdates.remove("saveLogsCSV");
                }
        );
    }

    @Override
    public void setLayout() {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(storageSelectorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(defaultSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(customSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(
                                                layout.createSequentialGroup()
                                                        .addComponent(folderSelectorButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(selectedFolderLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        )
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(storageSelectorLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(defaultSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(customSaveOption, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(
                                layout.createParallelGroup()
                                        .addComponent(folderSelectorButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(selectedFolderLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
        );
    }
}
