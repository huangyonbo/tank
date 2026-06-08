echo off
echo #############开始转化############
java -jar json2xml.jar
echo #############转化结束#############
echo #############拷贝文件#############
SET tartgetPath=%cd%\res\Game
del %tartgetPath%\PathPointTable.xml
del %tartgetPath%\FishGroupTable.xml
del %tartgetPath%\FishFormationTable.xml
move %cd%\PathPointTable.xml %tartgetPath%\PathPointTable.xml
move %cd%\FishGroupTable.xml %tartgetPath%\FishGroupTable.xml
move %cd%\FishFormationTable.xml %tartgetPath%\FishFormationTable.xml
pause