cd $1
for file in `ls`
do
	if [ -f $file ]
	then
		echo $file
		iconv -f gbk -t utf8 $file > ${file}_new
		mv ${file}_new $file
	fi
done
