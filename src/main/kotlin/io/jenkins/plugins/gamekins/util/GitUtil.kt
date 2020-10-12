package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.tasks.Mailer.UserProperty
import io.jenkins.plugins.gamekins.GameUserProperty
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import jenkins.security.MasterToSlaveCallable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import java.io.*
import java.util.*
import kotlin.jvm.Throws

object GitUtil {

    const val DEFAULT_SEARCH_COMMIT_COUNT = 50

    @JvmStatic
    fun getBranch(workspace: FilePath): String {
        try {
            return workspace.act(BranchCallable(workspace.remote))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getCanonicalTreeParser(git: Git, commitId: ObjectId): AbstractTreeIterator {
        val walk = RevWalk(git.repository)
        val commit = walk.parseCommit(commitId)
        val treeId = commit.tree.id
        val reader = git.repository.newObjectReader()
        return CanonicalTreeParser(null, reader, treeId)
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getCommit(repo: Repository, hash: String): RevCommit {
        val walk = RevWalk(repo)
        val id = repo.resolve(hash)
        val commit = walk.parseCommit(id)
        walk.dispose()
        return commit
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getDiffOfCommit(git: Git, repo: Repository, newCommit: RevCommit): String {
        val oldCommit = getPrevHash(repo, newCommit)
        val oldTreeIterator = if (oldCommit == null) EmptyTreeIterator() else getCanonicalTreeParser(git, oldCommit)
        val newTreeIterator = getCanonicalTreeParser(git, newCommit)
        val outputStream: OutputStream = ByteArrayOutputStream()
        val formatter = DiffFormatter(outputStream)

        formatter.setRepository(git.repository)
        formatter.format(oldTreeIterator, newTreeIterator)
        return outputStream.toString()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getHead(repo: Repository): RevCommit {
        return getCommit(repo, Constants.HEAD)
    }

    //TODO: Not performant - maybe JGit starts to search from the HEAD every time?
    //TODO: Looking at the time it seems random...
    //TODO: Maybe some commits have many parents?
    @JvmStatic
    @Throws(IOException::class)
    fun getLastChangedClasses(count: Int, constants: HashMap<String, String>, listener: TaskListener,
                              users: ArrayList<GameUser>, workspace: FilePath): ArrayList<ClassDetails> {
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(workspace.remote + "/.git"))
                .setMustExist(true).build()
        val walk = RevWalk(repo)
        val headCommit = getHead(repo)
        val git = Git(repo)
        var totalCount = 0
        var currentCommits = HashSet<RevCommit>()
        val searchedCommits = HashSet<RevCommit>()
        currentCommits.add(headCommit)
        val classes = ArrayList<ClassDetails>()
        val authorMapping = HashMap<PersonIdent, GameUser>()
        while (totalCount < count) {
            listener.logger.println("[Gamekins] Searched through $totalCount Commits")
            if (currentCommits.isEmpty()) break
            val newCommits = HashSet<RevCommit>()
            for (commit in currentCommits) {
                searchedCommits.add(commit)
                val diff = getDiffOfCommit(git, repo, commit)
                val lines = diff.split("\n".toRegex())
                for (i in lines.indices) {
                    val line = lines[i]
                    if (commit.shortMessage.toLowerCase().contains("merge")) break
                    //TODO: Shows diff of some merge requests, but not all
                    if (line.contains("diff --git") && i + 1 < lines.size && !lines[i + 1].contains("deleted")) {
                        val path = line.split(" ".toRegex())[2].substring(1)
                        val pathSplit = path.split("/".toRegex())
                        if (path.split("/".toRegex()).contains("test")
                                || !(path.contains(".java") || path.contains(".kt"))) {
                            continue
                        }
                        val classname = pathSplit[pathSplit.size - 1].split("\\.".toRegex())[0]
                        var found = false
                        for (details in classes) {
                            if (details.className == classname) {
                                var user = authorMapping[commit.authorIdent]
                                if (user == null) {
                                    user = mapUser(commit.authorIdent, users)
                                    if (user == null) {
                                        found = true
                                        break
                                    }
                                    authorMapping[commit.authorIdent] = user
                                }
                                details.addUser(user)
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            val details = ClassDetails(workspace, path,
                                    constants["jacocoResultsPath"]!!, constants["jacocoCSVPath"]!!, listener)
                            var user = authorMapping[commit.authorIdent]
                            if (user == null) {
                                user = mapUser(commit.authorIdent, users)
                                if (user == null) continue
                                authorMapping[commit.authorIdent] = user
                            }
                            details.addUser(user)
                            classes.add(details)
                        }
                    }
                }
                for (parent in commit.parents) {
                    if (!searchedCommits.contains(parent) && !newCommits.contains(parent)
                            && !currentCommits.contains(parent)) {
                        newCommits.add(walk.parseCommit(repo.resolve(parent.name)))
                    }
                    walk.dispose()
                }
                totalCount++
            }
            currentCommits = HashSet(newCommits)
        }
        return classes
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getLastChangedFilesOfUser(workspace: String, user: GameUser, commitCount: Int,
                                          commitHash: String, users: ArrayList<GameUser>): Set<String> {
        var commitSearchCount = commitCount
        if (commitSearchCount <= 0) commitSearchCount = DEFAULT_SEARCH_COMMIT_COUNT
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File("$workspace/.git")).setMustExist(true).build()
        val walk = RevWalk(repo)
        var targetCommit: RevCommit? = null
        if (commitHash.isNotEmpty()) {
            targetCommit = getCommit(repo, commitHash)
        }
        val headCommit = getHead(repo)
        val git = Git(repo)
        if (targetCommit === headCommit || targetCommit == null) return LinkedHashSet()
        var countUserCommit = 0
        var totalCount = 0
        var currentCommits = HashSet<RevCommit>()
        val searchedCommits = HashSet<RevCommit>()
        searchedCommits.add(targetCommit)
        searchedCommits.addAll(targetCommit.parents)
        var nearToLeaf = true
        for (revCommit in targetCommit.parents) {
            val parents = revCommit.parents
            if (parents != null && parents.isNotEmpty()) {
                nearToLeaf = false
                searchedCommits.addAll(parents)
            }
        }
        currentCommits.add(headCommit)
        val pathsToFiles = LinkedHashSet<String>()
        while (countUserCommit < commitSearchCount && totalCount < commitSearchCount * 5) {
            if (currentCommits.isEmpty()) break
            val newCommits = ArrayList<RevCommit>()
            for (commit in currentCommits) {
                searchedCommits.add(commit)
                val mapUser = mapUser(commit.authorIdent, users)
                if (mapUser != null && mapUser == user) {
                    val diff = getDiffOfCommit(git, repo, commit)
                    val lines = diff.split("\n".toRegex())
                    for (line in lines) {
                        if (line.contains("diff --git")) {
                            pathsToFiles.add(line.split(" ".toRegex())[2].substring(1))
                        }
                    }
                    countUserCommit++
                }
                for (parent in commit.parents) {
                    newCommits.add(walk.parseCommit(repo.resolve(parent.name)))
                    walk.dispose()
                }
            }
            if (nearToLeaf && newCommits.contains(targetCommit)) break
            newCommits.removeAll(searchedCommits)
            currentCommits = HashSet(newCommits)
            totalCount++
        }
        return pathsToFiles
    }

    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getLastChangedSourceFilesOfUser(workspace: FilePath, user: User, commitCount: Int,
                                        commitHash: String, users: Collection<User>): Set<String> {
        val pathsToFiles: MutableSet<String> = workspace.act(
                LastChangedFilesCallable(
                        workspace.remote, GameUser(user), commitCount, commitHash, mapUsersToGameUsers(users)
                )) as MutableSet<String>
        if (pathsToFiles.isNotEmpty()) {
            pathsToFiles.removeIf { path: String -> path.split("/".toRegex()).contains("test") }
            pathsToFiles.removeIf { path: String -> !(path.contains(".java") || path.contains(".kt")) }
        }
        return pathsToFiles
    }

    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getLastChangedTestFilesOfUser(workspace: FilePath, user: User, commitCount: Int,
                                      commitHash: String, users: Collection<User>): Set<String> {
        val pathsToFiles: MutableSet<String> = workspace.act(
                LastChangedFilesCallable(
                        workspace.remote, GameUser(user), commitCount, commitHash, mapUsersToGameUsers(users)
                )) as MutableSet<String>
        if (pathsToFiles.isNotEmpty()) {
            pathsToFiles.removeIf { path: String -> !path.split("/".toRegex()).contains("test") }
            pathsToFiles.removeIf { path: String -> !(path.contains(".java") || path.contains(".kt")) }
        }
        return pathsToFiles
    }

    @JvmStatic
    @Throws(IOException::class)
    private fun getPrevHash(repo: Repository, commit: RevCommit): RevCommit? {
        val walk = RevWalk(repo)

        walk.markStart(commit)
        for ((count, rev) in walk.withIndex()) {
            if (count == 1) {
                return rev
            }
        }
        walk.dispose()

        return null
    }

    @JvmStatic
    fun mapUser(ident: PersonIdent, users: Collection<User>): User? {
        val split = ident.name.split(" ".toRegex())
        for (user in users) {
            if (user.getProperty(Details::class.java) == null) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.getGitNames().contains(ident.name)
                    || user.fullName.contains(split[0]) && user.fullName.contains(split[split.size - 1])
                    || (user.getProperty(UserProperty::class.java) != null
                            && ident.emailAddress == user.getProperty(UserProperty::class.java).address)) {
                return user
            }
        }
        return null
    }

    @JvmStatic
    fun mapUser(ident: PersonIdent, users: ArrayList<GameUser>): GameUser? {
        val split = ident.name.split(" ".toRegex())
        for (user in users) {
            if (user.gitNames.contains(ident.name)
                    || user.fullName.contains(split[0]) && user.fullName.contains(split[split.size - 1])
                    || ident.emailAddress == user.mail) {
                return user
            }
        }
        return null
    }

    @JvmStatic
    fun mapUsersToGameUsers(users: Collection<User>): ArrayList<GameUser> {
        val gameUsers = ArrayList<GameUser>()
        for (user in users) {
            if (user.getProperty(Details::class.java) == null) continue
            gameUsers.add(GameUser(user))
        }
        return gameUsers
    }

    private class BranchCallable(private val workspace: String) : MasterToSlaveCallable<String, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        override fun call(): String {
            val builder = FileRepositoryBuilder()
            try {
                val repo = builder.setGitDir(File("$workspace/.git")).setMustExist(true).build()
                return repo.branch
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return ""
        }
    }

    class HeadCommitCallable(private val workspace: String) : MasterToSlaveCallable<RevCommit, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Throws(IOException::class)
        override fun call(): RevCommit {
            val builder = FileRepositoryBuilder()
            val repo = builder.setGitDir(File("$workspace/.git")).setMustExist(true).build()
            return getHead(repo)
        }
    }

    class LastChangedClassesCallable(private val count: Int, private val constants: HashMap<String, String>,
                                     private val listener: TaskListener, private val users: ArrayList<GameUser>,
                                     private val workspace: FilePath)
        : MasterToSlaveCallable<ArrayList<ClassDetails>, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Throws(IOException::class)
        override fun call(): ArrayList<ClassDetails> {
            return getLastChangedClasses(count, constants, listener, users,
                    workspace)
        }
    }

    private class LastChangedFilesCallable constructor(private val workspace: String, private val user: GameUser,
                                                       private val commitCount: Int, private val commitHash: String,
                                                       private val users: ArrayList<GameUser>)
        : MasterToSlaveCallable<Set<String>, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Throws(IOException::class)
        override fun call(): Set<String> {
            return getLastChangedFilesOfUser(workspace, user, commitCount, commitHash, users)
        }
    }

    class GameUser(user: User) : Serializable {

        val id: String = user.id
        val fullName: String = user.fullName
        val mail: String =
                if (user.getProperty(UserProperty::class.java) == null) ""
                else user.getProperty(UserProperty::class.java).address
        val gitNames: HashSet<String> =
                if (user.getProperty(GameUserProperty::class.java) == null) HashSet()
                else HashSet(user.getProperty(GameUserProperty::class.java).getGitNames())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val gameUser = other as GameUser
            return id == gameUser.id && fullName == gameUser.fullName && mail == gameUser.mail && gitNames == gameUser.gitNames
        }

        //TODO: Not callable on remote machines
        fun getUser(): User? {
            for (user in User.getAll()) {
                if (user.getProperty(Details::class.java) == null) continue
                if (user.id == id) return user
            }
            return null
        }

        override fun hashCode(): Int {
            return Objects.hash(id, fullName, mail, gitNames)
        }
    }
}
