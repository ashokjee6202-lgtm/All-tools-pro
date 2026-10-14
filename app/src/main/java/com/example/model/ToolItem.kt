package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Transform
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val titleKey: String) {
    POPULAR("popular_tools"),
    SCANNER("cat_scanner"),
    PDF("cat_pdf"),
    PHOTO("cat_photo"),
    HANDWRITING("cat_handwriting"),
    QR("cat_qr"),
    UTILITIES("cat_utilities")
}

data class ToolItem(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val isPopular: Boolean = false
)

object ToolRegistry {
    val allTools = listOf(
        ToolItem(
            id = "doc_scanner",
            titleKey = "tool_doc_scanner",
            descriptionKey = "tool_doc_scanner_desc",
            category = ToolCategory.SCANNER,
            icon = Icons.Default.CenterFocusStrong,
            isPopular = true
        ),
        ToolItem(
            id = "photo_resize",
            titleKey = "tool_photo_resize",
            descriptionKey = "tool_photo_resize_desc",
            category = ToolCategory.PHOTO,
            icon = Icons.Default.Transform,
            isPopular = true
        ),
        ToolItem(
            id = "pdf_compress",
            titleKey = "tool_pdf_compress",
            descriptionKey = "tool_pdf_compress_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.Compress,
            isPopular = true
        ),
        ToolItem(
            id = "pdf_resize",
            titleKey = "tool_pdf_resize",
            descriptionKey = "tool_pdf_resize_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.Crop,
            isPopular = false
        ),
        ToolItem(
            id = "img_to_pdf",
            titleKey = "tool_img_to_pdf",
            descriptionKey = "tool_img_to_pdf_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.PictureAsPdf,
            isPopular = true
        ),
        ToolItem(
            id = "pdf_to_img",
            titleKey = "tool_pdf_to_img",
            descriptionKey = "tool_pdf_to_img_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.Image,
            isPopular = true
        ),
        ToolItem(
            id = "merge_pdf",
            titleKey = "tool_merge_pdf",
            descriptionKey = "tool_merge_pdf_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.MergeType,
            isPopular = true
        ),
        ToolItem(
            id = "split_pdf",
            titleKey = "tool_split_pdf",
            descriptionKey = "tool_split_pdf_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.Description,
            isPopular = false
        ),
        ToolItem(
            id = "watermark_pdf",
            titleKey = "tool_watermark_pdf",
            descriptionKey = "tool_watermark_pdf_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.FormatColorText,
            isPopular = false
        ),
        ToolItem(
            id = "handwriting",
            titleKey = "tool_handwriting",
            descriptionKey = "tool_handwriting_desc",
            category = ToolCategory.HANDWRITING,
            icon = Icons.Default.Draw,
            isPopular = true
        ),
        ToolItem(
            id = "qr_generator",
            titleKey = "tool_qr_generator",
            descriptionKey = "tool_qr_generator_desc",
            category = ToolCategory.QR,
            icon = Icons.Default.QrCode,
            isPopular = true
        ),
        ToolItem(
            id = "qr_scanner",
            titleKey = "tool_qr_scanner",
            descriptionKey = "tool_qr_scanner_desc",
            category = ToolCategory.QR,
            icon = Icons.Default.QrCodeScanner,
            isPopular = false
        ),
        ToolItem(
            id = "sign_pdf",
            titleKey = "tool_sign_pdf",
            descriptionKey = "tool_sign_pdf_desc",
            category = ToolCategory.UTILITIES,
            icon = Icons.Default.Edit,
            isPopular = false
        ),
        ToolItem(
            id = "passport_photo",
            titleKey = "tool_passport_photo",
            descriptionKey = "tool_passport_photo_desc",
            category = ToolCategory.PHOTO,
            icon = Icons.Default.Crop,
            isPopular = false
        ),
        ToolItem(
            id = "blank_pdf",
            titleKey = "tool_blank_pdf",
            descriptionKey = "tool_blank_pdf_desc",
            category = ToolCategory.PDF,
            icon = Icons.Default.Description,
            isPopular = false
        ),
        ToolItem(
            id = "ocr_tool",
            titleKey = "tool_ocr",
            descriptionKey = "tool_ocr_desc",
            category = ToolCategory.UTILITIES,
            icon = Icons.Default.Calculate,
            isPopular = false
        )
    )

    fun getPopularTools(): List<ToolItem> = allTools.filter { it.isPopular }
}
