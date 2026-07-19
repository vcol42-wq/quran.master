import re

file_path = 'app/src/main/res/layout/activity_athan.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove layout_weight=1 and 0dp from prayer names
content = content.replace('android:layout_width="0dp"\n                        android:layout_height="wrap_content"\n                        android:layout_weight="1"', 'android:layout_width="wrap_content"\n                        android:layout_height="wrap_content"\n                        android:layout_marginEnd="16dp"')

# Change gravity of the row from center_vertical to center to center everything nicely
content = re.sub(r'(android:id="@+id/layout(?:Fajr|Sunrise|Dhuhr|Asr|Maghrib|Isha)"[\s\S]*?)android:gravity="center_vertical"', r'\1android:gravity="center"', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
