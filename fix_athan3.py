import re

file_path = 'app/src/main/res/layout/activity_athan.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace margin and padding in the stepper layout
old_stepper = 'android:layout_marginEnd="16dp" android:paddingHorizontal="12dp"'
new_stepper = 'android:layout_marginEnd="4dp" android:paddingHorizontal="2dp"'
content = content.replace(old_stepper, new_stepper)

# Reduce minWidth on the offset text
content = content.replace('android:minWidth="24dp"', 'android:minWidth="16dp"')

# Reduce horizontal padding on the pill itself from 16dp to 8dp to give more room
content = content.replace('android:paddingHorizontal="16dp"\n                    android:layout_marginBottom="12dp"', 'android:paddingHorizontal="8dp"\n                    android:layout_marginBottom="12dp"')

# Reduce the time text size just a tiny bit to 16sp
content = content.replace('android:textSize="18sp"\n                        android:textStyle="bold"\n                        android:fontFamily="sans-serif-black"\n                        android:textColor="#1B4332"', 'android:textSize="16sp"\n                        android:textStyle="bold"\n                        android:fontFamily="sans-serif-black"\n                        android:textColor="#1B4332"')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Done!')
