package fastcampus.aop.part4.aop_part4_chapter06.data.models.airquality

import androidx.annotation.ColorRes
import com.google.gson.annotations.SerializedName
import fastcampus.aop.part4.aop_part4_chapter06.R

enum class Grade(
    val label: String,
    val emoji: String,
    @ColorRes
    val colorResId: Int
) {
    @SerializedName("1")
    GOOD("좋음", "😊", colorResId = R.color.blue),

    @SerializedName("2")
    NORMAL("보통", "😃",colorResId = R.color.green),

    @SerializedName("3")
    BAD("나쁨", "😠", colorResId =R.color.yellow),

    @SerializedName("4")
    AWFUL("매우 나쁨", "😡", colorResId =R.color.red),

    UNKNOWN("미측정", "🤔",colorResId = R.color.gray);

    override fun toString(): String {
        return "$label $emoji"
    }
}