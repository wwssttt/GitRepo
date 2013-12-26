#!/usr/bin python
#-coding:utf-8----
###########################
#python getfile.py dirname filetype keywords
###########################
import os,sys

find_text = sys.argv

diro = find_text[1]
filetype = find_text[2]

print '\n搜索目录 %s' % diro
print '搜索文件类型 %s' % filetype
print '关键字为: ',' '.join(find_text[3:])

print '\n\n查询到包含关键字的文件是:\n'
for root,subdirs,filename in os.walk(diro):
    for i in filename:
        if(i.endswith(filetype)):
            infile = open(root+'/'+i,'r')
            content = infile.read().lower()
            sts = True
            for j in find_text[3:]:
                j = j.lower()
                if j in content:
                    sts = True
                else:
                    sts = False
                    break
            infile.close()
            if sts == True:
                print root+'/'+i
