package doritos.common.vcs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.vcs.commit.ChangeListCommitState
import com.intellij.vcs.commit.SingleChangeListCommitter
import com.intellij.vcs.commit.isAmendCommitMode
import com.intellij.vcs.log.VcsLogProvider
import java.util.concurrent.CompletableFuture

class CommitOnSaveAction: AnAction(AllIcons.Actions.Commit) {
    override fun actionPerformed(event: AnActionEvent) {
        doCommitOnSave(event.project ?: return)
    }
}

fun doCommitOnSave(project: Project, isAmendCommit: Boolean = false, onSuccess: () -> Unit = {}) {
    val defaultChangeList = project.defaultChangeList() ?: return
    val changes = defaultChangeList.changes.toList()

    val commitMessage = VcsConfiguration.getInstance(project).LAST_COMMIT_MESSAGE ?: "Commit on save"

    LineStatusTrackerManager.getInstanceImpl(project).resetExcludedFromCommitMarkers()

    FileDocumentManager.getInstance().saveAllDocuments()

    val committer = SingleChangeListCommitter(
        project,
        ChangeListCommitState(defaultChangeList as LocalChangeList, changes, commitMessage),
        createCommitContext(isAmendCommit),
        "Commit on save",
        isDefaultChangeListFullyIncluded = true
    )
    committer.addResultHandler { onSuccess() }
    committer.runCommit("Commit on save", sync = false)
}

private fun createCommitContext(isAmendCommit: Boolean): CommitContext =
    CommitContext().also {
        if (isAmendCommit) {
            it.isAmendCommitMode
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            it.putUserData(Key.findKeyByName("Vcs.Commit.IsAmendCommitMode") as Key<Boolean>, true)
        }
    }

fun Project.defaultChangeList(): ChangeList? =
    if (!ProjectLevelVcsManager.getInstance(this).hasActiveVcss()) null
    else ChangeListManager.getInstance(this).defaultChangeList
