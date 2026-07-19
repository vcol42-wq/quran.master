import re

file_path = 'app/src/main/res/layout/activity_athan.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Change gravity of the row back to center_vertical instead of center
content = re.sub(r'(android:id="@+id/layout(?:Fajr|Sunrise|Dhuhr|Asr|Maghrib|Isha)"[\s\S]*?)android:gravity="center"', r'\1android:gravity="center_vertical"', content)

# 2. Vertical compression:
# paddingVertical from 16dp to 8dp
content = content.replace('android:paddingVertical="16dp"', 'android:paddingVertical="8dp"')
content = content.replace('android:paddingVertical="10dp"', 'android:paddingVertical="8dp"') # just in case
# marginBottom from 12dp to 6dp
content = content.replace('android:layout_marginBottom="12dp"', 'android:layout_marginBottom="6dp"')

# 3. Horizontal distribution:
# Stepper:
stepper_pattern = r'(<LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:layout_marginEnd="4dp" android:paddingHorizontal="2dp" android:paddingVertical="2dp" >)'
space_xml = '<Space android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1"/>\n                    '
content = re.sub(stepper_pattern, space_xml + r'\1', content)

# Time:
time_pattern = r'(<TextView\s+android:id="@+id/tvTime)'
content = re.sub(time_pattern, space_xml + r'\1', content)

# Remove the marginEnd=16dp from the Name TextView so it touches the right edge naturally
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="الفجر"', 'android:text="الفجر"')
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="الشروق"', 'android:text="الشروق"')
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="الظهر"', 'android:text="الظهر"')
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="العصر"', 'android:text="العصر"')
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="المغرب"', 'android:text="المغرب"')
content = content.replace('android:layout_marginEnd="16dp"\n                        android:text="العشاء"', 'android:text="العشاء"')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
