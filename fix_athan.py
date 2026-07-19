import re

file_path = 'app/src/main/res/layout/activity_athan.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('android:textSize="24sp"', 'android:textSize="18sp"')
content = content.replace('android:background="@drawable/bg_stepper"', '')
content = content.replace('android:textSize="20sp"', 'android:textSize="18sp"')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
