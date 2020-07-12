package io.jenkins.plugins.gamekins.property;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.util.ListBoxModel;
import jenkins.branch.MultiBranchProject;
import jenkins.branch.OrganizationFolder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.*;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GameOrganizationFolderProperty extends AbstractFolderProperty<AbstractFolder<?>> {

    private GameOrganizationFolderProperty() { }

    @Extension
    public static class DescriptorFolderImpl extends AbstractFolderPropertyDescriptor {

        public DescriptorFolderImpl() {
            super();
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Set the activation of the Gamekins plugin.";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractFolder> containerType) {
            return containerType == OrganizationFolder.class;
        }

        @Override
        public AbstractFolderProperty<?> newInstance(StaplerRequest req, JSONObject formData) {
            if (req == null || formData == null) return null;
            OrganizationFolder folder = (OrganizationFolder) req.findAncestor(OrganizationFolder.class).getObject();
            for (MultiBranchProject<?, ?> project: folder.getItems()) {
                if (project.getName().equals(formData.getString("project"))) {
                    try {
                        GameMultiBranchProperty property = project.getProperties().get(GameMultiBranchProperty.class);
                        if (property == null) {
                            project.addProperty(new GameMultiBranchProperty(project,
                                    formData.getBoolean("activated"), formData.getBoolean("showStatistics")));
                        } else {
                            property.reconfigure(req, formData);
                        }
                        folder.save();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        public ListBoxModel doFillProjectItems(@AncestorInPath OrganizationFolder job) {
            if (job == null) return new ListBoxModel();
            ListBoxModel listBoxModel = new ListBoxModel();
            job.getItems().stream().map(AbstractItem::getName).forEach(listBoxModel::add);
            return listBoxModel;
        }
    }
}
