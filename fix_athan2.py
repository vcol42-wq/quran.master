import re

file_path = 'app/src/main/res/layout/activity_athan.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i in range(len(lines)):
    line = lines[i]
    if 'android:textSize="18sp"' in line:
        if ('btnDec' in line or 'btnInc' in line):
            lines[i] = line.replace('18sp', '16sp')
        elif i > 0 and ('tvTime' in lines[i-1] or 'tvTime' in lines[i-2] or 'tvTime' in lines[i-3]):
            lines[i] = line.replace('18sp', '16sp')
        elif i > 0 and ('text="الفجر"' in lines[i-1] or 'text="الشروق"' in lines[i-1] or 'text="الظهر"' in lines[i-1] or 'text="العصر"' in lines[i-1] or 'text="المغرب"' in lines[i-1] or 'text="العشاء"' in lines[i-1]):
            lines[i] = line.replace('18sp', '16sp')
            
    if 'android:textSize="14sp"' in line and 'tvOffset' in line:
        lines[i] = line.replace('14sp', '12sp')

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Done!')
