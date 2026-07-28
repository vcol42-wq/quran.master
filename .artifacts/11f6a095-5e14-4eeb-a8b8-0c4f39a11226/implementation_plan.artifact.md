# خطة تعديل مظهر أرقام الآيات وإصلاح مؤشرات الصفحات

تهدف هذه الخطة إلى خفض موقع دوائر أرقام الآيات لتتناسب مع خط الكتابة، وإصلاح مشكلة عدم ظهور دوائر تمييز جهة الصفحة (يمين/يسار المستخدم).

## التغييرات المقترحة

### [Component] محول البيانات (Quran Adapter)

#### [MODIFY] [QuranAdapter.kt](file:///C:/quran.master/app/src/main/java/com/sabah/bikhushue/QuranAdapter.kt)
- **تعديل تلوين أرقام الآيات:**
    - في كلاس `BeautifiedAyahEndSpan`:
        - إضافة إزاحة (Offset) بسيطة للأسفل لمتغير `centerY` و `textY` لضمان محاذاة الدائرة مع الحروف.
        - تصغير القطر قليلاً ليكون أكثر دقة.
- **إصلاح مؤشرات الصفحات:**
    - التأكد من أن `pageNum` يبدأ من 1 (بناءً على بيانات المصحف).
    - رفع درجة وضوح اللون الأخضر والأحمر في البرمجة.

### [Component] واجهة العناصر (UI Layout)

#### [MODIFY] [item_verse.xml](file:///C:/quran.master/app/src/main/res/layout/item_verse.xml)
- تحديث حاوية رقم الصفحة والمؤشرات:
    - زيادة الهامش العلوي (`layout_marginTop`) لضمان عدم التداخل.
    - التأكد من أن `indicatorLeft` و `indicatorRight` ظاهران تماماً عبر ضبط `layout_width` و `layout_height`.

### [Component] الموارد (Resources)

#### [MODIFY] [bg_circle_indicator_green_soft.xml](file:///C:/quran.master/app/src/main/res/drawable/bg_circle_indicator_green_soft.xml) & [red_soft](file:///C:/quran.master/app/src/main/res/drawable/bg_circle_indicator_red_soft.xml)
- استخدام ألوان صريحة (Solid Colors) بدون أي شفافية لضمان الظهور.

---

## خطة التحقق (Verification Plan)

### التحقق اليدوي البصري:
- **أرقام الآيات:** التأكد من أنها "نزلت" قليلاً لتصبح في منتصف سطر الكتابة.
- **مؤشرات الصفحات:** الانتقال للصفحة 3 والتأكد من رؤية الدائرة الخضراء (اليمين) بوضوح تام، والصفحة 4 للدائرة الحمراء (اليسار).
- **الثيمات:** التأكد من وضوح هذه العناصر في كافة الثيمات.
