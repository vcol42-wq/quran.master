package com.sabah.bikhushue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuranLineSeparator(
    currentSeparator: SeparatorType,
    pageNumber: Int?,
    rukooTotal: Int?,
    rukooSura: Int?
) {
    when (currentSeparator) {
        SeparatorType.PAGE -> {
            if (pageNumber != null) {
                PageNumberSeparator(pageNumber)
            }
        }
        SeparatorType.RUKOO_KHATMA_29, SeparatorType.RUKOO_KHATMA_30 -> {
            if (rukooTotal != null && rukooTotal > 0) {
                RukooBadgeSeparator(
                    total = rukooTotal, 
                    suraRukoo = rukooSura ?: 1,
                    isKhatma30 = (currentSeparator == SeparatorType.RUKOO_KHATMA_30)
                )
            }
        }
        SeparatorType.NONE, SeparatorType.HIZB, SeparatorType.MANZIL -> { /* لا شيء */ }
    }
}

@Composable
fun PageNumberSeparator(pageNumber: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ص $pageNumber",
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RukooBadgeSeparator(
    total: Int,
    suraRukoo: Int,
    isKhatma30: Boolean = false,
    goldColor: Color = Color(0xFFD2B48C),
    emeraldColor: Color = Color(0xFF00A86B),
    bgColor: Color = Color(0xFFFAF3E0)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontal Wing Right (يمتد لأقصى اليمين)
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = goldColor
        )

        // Center Geometric Badge (شكل بيضاوي/مستطيل بحواف دائرية تتصل به الأجنحة)
        Row(
            modifier = Modifier
                .background(bgColor, shape = RoundedCornerShape(20.dp))
                .border(1.5.dp, goldColor, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // الجناح الأيمن: رقم الركوع التراكمي الشامل
            Text(
                text = "$total",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = emeraldColor,
                modifier = Modifier.padding(end = 8.dp)
            )

            // الوسط: دائرة بارزة بداخلها حرف "ع"
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(emeraldColor, shape = CircleShape)
                    .border(1.dp, goldColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ع",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // الجناح الأيسر: رقم الركوع المحلي بداخل السورة
            Text(
                text = "$suraRukoo",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = emeraldColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Horizontal Wing Left (يمتد لأقصى اليسار)
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.5.dp,
            color = goldColor
        )
    }
}
