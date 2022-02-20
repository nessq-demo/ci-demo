package es.us.isa.restest.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
 
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
 
public class BitBucket {
 
	public static void main(String[] args) throws Exception
	{
		String repoURLWithCredentials = "https://rajamanthiram:Vadapalani12345$@bitbucket.org/procurement_zycus/coe_automation.git";
		
		String branchName = "restest_develop";
		String checkoutDir = "tempDir";
		String releaseName = "Mars";
		String testDir = "src/generation/java/zycus_new";
		BitBucket bitBucket = new BitBucket(repoURLWithCredentials, branchName, checkoutDir,releaseName, testDir);
		bitBucket.checkInReleaseTestCases();
	}
	
	private String checkoutDir;
	private String branchName;
	private String repoURLWithCredentials;
	private String releaseName;
	private String testDir;
	
	public BitBucket(String repoURLWithCredentials, String branchName, String checkoutDir, String releaseName, String testDir)
	{
		this.repoURLWithCredentials = repoURLWithCredentials;
		this.branchName = branchName;
		this.checkoutDir = checkoutDir;
		this.releaseName = releaseName;
		this.testDir = testDir;
		
	}
	
	public void checkInReleaseTestCases() throws IOException, InvalidRemoteException, TransportException, GitAPIException, Exception
	{
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMMYYYY_hhmmss");
		Calendar cal = Calendar.getInstance();
		
		String commitMessage = "Commiting Release '"+releaseName+"' test cases on "+dateFormat.format(cal.getTime());
		
		// Local directory on this machine where we will clone remote repo.
		File localRepoDir = new File("./"+checkoutDir);

		if(localRepoDir.exists())
		{
			FileUtils.deleteDirectory(localRepoDir);
		}
		
		// Monitor to get git command progress printed on java System.out console
		TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
 
		System.out.println("\n>>> Cloning repository\n");

		String gitBranch = "refs/heads/"+branchName;
		Git git1 = Git.cloneRepository()
				  .setProgressMonitor(consoleProgressMonitor)	
				  .setURI(repoURLWithCredentials)
				  .setDirectory( localRepoDir )
				  .setBranchesToClone( Arrays.asList(new String[] { gitBranch }))
				  .setBranch(gitBranch)
				  .call();
		
		try (Git git = git1) 
		{
 
			Optional<String> developBranch = git.branchList().setListMode(ListMode.REMOTE).call().stream()
					.map(r -> r.getName()).filter(n -> n.contains(branchName)).findAny();
 
			if (developBranch.isPresent()) 
			{
				System.out.println("\n>>> Checking out the branch\n");

				git.checkout().setProgressMonitor(consoleProgressMonitor).setName(branchName).setStartPoint("HEADˆ").call();
			}
			else	
			{
				System.out.println("\n>>> Invalid branch '"+branchName+"'. Check the branchName given\n");
				System.exit(-1);
				
			}
			
			//testSetup();
			
			addReleaseTCFiles(git);
			addReleaseXls(git);

			System.out.println("\n>>> Printing status of local repository\n");
			Status status = git.status().setProgressMonitor(consoleProgressMonitor).call();
			System.out.println("Modified file = " + status.getModified());
			System.out.println("Added: " + status.getAdded());
			System.out.println("Uncommitted: " + status.getUncommittedChanges());
			System.out.println("Untracked: " + status.getUntracked());

			System.out.println("\n>>> Committing changes\n");
			RevCommit revCommit = git.commit().setAll(true).setMessage(commitMessage).call();
			System.out.println("Commit = " + revCommit.getFullMessage());
			git.push().call();
			

	      }
	}
	
	public void addReleaseTCFiles(Git git) throws Exception
	{
		
		File currentDir = new File(".");
		
		File sourceDir = new File(currentDir.getAbsolutePath().substring(0, currentDir.getAbsolutePath().length()-1)+"/"+testDir+"/releases/"+releaseName);
		if(!sourceDir.isDirectory())
		{
			System.out.println("No Release Test cases in '"+sourceDir.getAbsolutePath()+"'. Exiting...");
			System.exit(-1);
		}
		
		File targetDir = new File(currentDir.getAbsolutePath().substring(0, currentDir.getAbsolutePath().length()-1)+"/"+checkoutDir+"/"+testDir+"/releases/"+releaseName);
		
		if(targetDir.exists())
		{
			FileUtils.deleteDirectory(targetDir);
		}
		
		FileUtils.copyDirectory(sourceDir, targetDir);
		
		List<File> files = (List<File>) FileUtils.listFiles(targetDir, null, true);
		
		String path;
		for(File file: files)
		{
			path = file.getAbsolutePath();
			path = path.substring(path.indexOf("src"));
			path = path.replaceAll("\\\\", "/");
			
			git.add().addFilepattern(path).call();
			
			System.out.println("Adding the file to BitBucket: "+path);
		}
	}
	
	
	public void addReleaseXls(Git git) throws Exception
	{
		
		File currentDir = new File(".");
		
		File sourceFile = new File(currentDir.getAbsolutePath().substring(0, currentDir.getAbsolutePath().length()-1)+"/"+testDir+"/releases/releases.xlsx");
		
		if(!sourceFile.exists())
		{
			return;
		}
		
		File targetFile = new File(currentDir.getAbsolutePath().substring(0, currentDir.getAbsolutePath().length()-1)+"/"+checkoutDir+"/"+testDir+"/releases/releases.xlsx");
	
		
		if(targetFile.exists())
		{
			FileUtils.forceDelete(targetFile);
		}
		
		FileUtils.copyFile(sourceFile, targetFile);
		
		File file = new File(currentDir.getAbsolutePath().substring(0, currentDir.getAbsolutePath().length()-1)+"/"+checkoutDir+"/"+testDir+"/releases/releases.xlsx");
		String path = file.getAbsolutePath();
		path = path.substring(path.indexOf("src"));
		path = path.replaceAll("\\\\", "/");
		git.add().addFilepattern(path).call();
			
		System.out.println("Adding the file to BitBucket: "+file.getAbsolutePath());
		
	}
	
	public void testSetup() throws Exception
	{
		File file1 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/test1.txt");
		FileUtils.forceDelete(file1);
		
		File file2 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/test2.txt");
		FileUtils.forceDelete(file2);
		
		File file4 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/test4.txt");
		FileUtils.forceDelete(file4);
		
		File file3 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/JSONs/test3.txt");
		FileUtils.forceDelete(file3);
		
		File file5 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/JSONs/test5.txt");
		FileUtils.forceDelete(file5);
		
		
		File file6 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/test6.txt");
		File file7 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/JSONs/test7.txt");
		
		File folder1 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars");
		FileUtils.forceMkdir(folder1);
		File folder2 = new File(checkoutDir+"/src/generation/java/zycus_new/releases/Mars/JSONs");
		FileUtils.forceMkdir(folder2);
		FileWriter writer = new FileWriter(file6);
		writer.append("Test6");
		writer.close();
		writer = new FileWriter(file7);
		writer.append("Test7");
		writer.close();
		
	}
 
}
