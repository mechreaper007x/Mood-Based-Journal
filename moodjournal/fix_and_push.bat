@echo off
echo Resolving conflict... > fix_log.txt
git add src/main/resources/application.properties >> fix_log.txt 2>&1
echo Untracking application.properties... >> fix_log.txt
git rm --cached src/main/resources/application.properties >> fix_log.txt 2>&1
echo Staging other files... >> fix_log.txt
git add . >> fix_log.txt 2>&1
echo Committing... >> fix_log.txt
git commit -m "Resolve merge conflict and stop tracking application.properties" >> fix_log.txt 2>&1
echo Pushing... >> fix_log.txt
git push >> fix_log.txt 2>&1
echo DONE >> fix_log.txt
type fix_log.txt
