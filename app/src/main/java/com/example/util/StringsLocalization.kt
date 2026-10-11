package com.example.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage {
    ENGLISH,
    HINDI
}

object LocaleManager {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
    }

    fun isHindi() = currentLanguage == AppLanguage.HINDI
}

object Strings {
    fun get(key: String): String {
        val isHi = LocaleManager.isHindi()
        return when (key) {
            "app_name" -> if (isHi) "ऑलटूल्स प्रो" else "AllToolsPro"
            "app_tagline" -> if (isHi) "ऑल-इन-वन पीडीएफ, फोटो और स्कैनर" else "All-in-One PDF, Photo & Scanner"
            "search_tools_placeholder" -> if (isHi) "टूल्स खोजें (उदा. कंप्रेस, स्कैनर, क्यूआर)..." else "Search tools (e.g. Compress, Scanner, QR)..."
            "popular_tools" -> if (isHi) "लोकप्रिय टूल्स" else "Popular Tools"
            "categories" -> if (isHi) "श्रेणियां" else "Categories"
            "recent_files" -> if (isHi) "हालिया फाइलें" else "Recent Files"
            "favorites" -> if (isHi) "पसंदीदा टूल्स" else "Favorites"
            "no_favorites" -> if (isHi) "कोई पसंदीदा टूल नहीं जोड़ा गया" else "No favorite tools added yet"
            "no_recent_files" -> if (isHi) "कोई हालिया फाइल उपलब्ध नहीं है" else "No recent files yet"
            
            // Categories
            "cat_scanner" -> if (isHi) "दस्तावेज़ स्कैनर" else "Document Scanner"
            "cat_pdf" -> if (isHi) "पीडीएफ टूल्स" else "PDF Tools"
            "cat_photo" -> if (isHi) "फोटो टूल्स" else "Photo Tools"
            "cat_handwriting" -> if (isHi) "टेक्स्ट से हस्तलेखन" else "Text to Handwriting"
            "cat_qr" -> if (isHi) "क्यूआर और बारकोड" else "QR & Barcode"
            "cat_utilities" -> if (isHi) "उपयोगिता टूल्स" else "Utility Tools"

            // Bottom Nav
            "nav_home" -> if (isHi) "होम" else "Home"
            "nav_tools" -> if (isHi) "टूल्स" else "Tools"
            "nav_files" -> if (isHi) "फाइलें" else "Files"
            "nav_favorites" -> if (isHi) "पसंदीदा" else "Favorites"
            "nav_settings" -> if (isHi) "सेटिंग्स" else "Settings"

            // Common actions
            "select_file" -> if (isHi) "फाइल चुनें" else "Select File"
            "select_images" -> if (isHi) "छवियां चुनें" else "Select Images"
            "select_pdf" -> if (isHi) "पीडीएफ चुनें" else "Select PDF"
            "camera" -> if (isHi) "कैमरा" else "Camera"
            "gallery" -> if (isHi) "गैलरी" else "Gallery"
            "process" -> if (isHi) "प्रोसेस करें" else "Process"
            "download" -> if (isHi) "डाउनलोड करें" else "Download"
            "share" -> if (isHi) "शेयर करें" else "Share"
            "reset" -> if (isHi) "रीसेट" else "Reset"
            "preview" -> if (isHi) "पूर्वावलोकन" else "Preview"
            "cancel" -> if (isHi) "रद्द करें" else "Cancel"
            "save" -> if (isHi) "सहेजें" else "Save"
            "delete" -> if (isHi) "हटाएं" else "Delete"
            "processing" -> if (isHi) "प्रक्रिया जारी है..." else "Processing..."
            "success" -> if (isHi) "सफल!" else "Success!"
            "error" -> if (isHi) "त्रुटि" else "Error"
            "original_size" -> if (isHi) "मूल आकार" else "Original Size"
            "output_size" -> if (isHi) "आउटपुट आकार" else "Output Size"
            "target_size" -> if (isHi) "लक्षित आकार" else "Target Size"
            "compression_ratio" -> if (isHi) "आकार में कमी" else "Reduction"
            "privacy_notice" -> if (isHi) "100% ऑन-डिवाइस सुरक्षित। आपकी फाइलें कभी सर्वर पर नहीं भेजी जाती हैं।" else "100% On-Device & Private. Files are processed locally and never uploaded."

            // Tool names
            "tool_doc_scanner" -> if (isHi) "दस्तावेज़ स्कैनर" else "Document Scanner"
            "tool_doc_scanner_desc" -> if (isHi) "कैमरे या गैलरी से कई पेज स्कैन करें और पीडीएफ में बदलें" else "Scan multi-page documents with filters & export to PDF"
            
            "tool_photo_resize" -> if (isHi) "फोटो रिसाइज व कंप्रेस" else "Photo Resizer"
            "tool_photo_resize_desc" -> if (isHi) "केबी/एमबी लक्ष्य या पिक्सल द्वारा फोटो का आकार बदलें" else "Resize by target KB/MB or dimensions with batch mode"

            "tool_pdf_compress" -> if (isHi) "पीडीएफ कंप्रेसर" else "PDF Compressor"
            "tool_pdf_compress_desc" -> if (isHi) "गुणवत्ता बनाए रखते हुए पीडीएफ फाइल साइज घटाएं" else "Compress PDF file size to target KB/MB"

            "tool_pdf_resize" -> if (isHi) "पीडीएफ पेज रिसाइजर" else "PDF Page Resizer"
            "tool_pdf_resize_desc" -> if (isHi) "A4, A5, Letter आदि में पेज आकार बदलें" else "Change page dimensions to A4, Letter, A5 etc."

            "tool_img_to_pdf" -> if (isHi) "छवि से पीडीएफ" else "Images to PDF"
            "tool_img_to_pdf_desc" -> if (isHi) "JPG/PNG छवियों को एक पीडीएफ फाइल में बदलें" else "Convert JPG/PNG images into a single PDF"

            "tool_pdf_to_img" -> if (isHi) "पीडीएफ से छवि" else "PDF to Images"
            "tool_pdf_to_img_desc" -> if (isHi) "पीडीएफ पेजों को JPG/PNG में बदलें" else "Extract PDF pages as high quality JPG/PNG images"

            "tool_merge_pdf" -> if (isHi) "पीडीएफ मर्ज करें" else "Merge PDF"
            "tool_merge_pdf_desc" -> if (isHi) "कई पीडीएफ फाइलों को एक में जोड़ें" else "Combine multiple PDF documents into one"

            "tool_split_pdf" -> if (isHi) "पीडीएफ विभाजित करें" else "Split PDF"
            "tool_split_pdf_desc" -> if (isHi) "पेज अलग करें या रेंज निकालें" else "Extract specific page ranges or single pages"

            "tool_watermark_pdf" -> if (isHi) "वॉटरमार्क और पेज नंबर" else "Watermark & Page No."
            "tool_watermark_pdf_desc" -> if (isHi) "पीडीएफ में टेक्स्ट वॉटरमार्क या पेज नंबर जोड़ें" else "Add text watermark, stamp or page numbering"

            "tool_sign_pdf" -> if (isHi) "हस्ताक्षर निर्माता" else "Signature Maker"
            "tool_sign_pdf_desc" -> if (isHi) "डिजिटल हस्ताक्षर बनाएं और पीएनजी/पीडीएफ में सहेजें" else "Draw and save digital signatures as PNG or PDF"

            "tool_handwriting" -> if (isHi) "टेक्स्ट से हस्तलेखन" else "Text to Handwriting"
            "tool_handwriting_desc" -> if (isHi) "टाइप किए गए टेक्स्ट को हस्तलिखित असाइनमेंट में बदलें" else "Convert typed notes into handwritten notebook pages"

            "tool_qr_generator" -> if (isHi) "क्यूआर कोड जेनरेटर" else "QR & Barcode Generator"
            "tool_qr_generator_desc" -> if (isHi) "UPI भुगतान, URL, टेक्स्ट और वाई-फाई क्यूआर बनाएं" else "Create UPI Payment, URL, WiFi & Text QR codes"

            "tool_qr_scanner" -> if (isHi) "क्यूआर व बारकोड स्कैनर" else "QR & Barcode Scanner"
            "tool_qr_scanner_desc" -> if (isHi) "कैमरे या छवि से क्यूआर और बारकोड स्कैन करें" else "Scan QR & barcodes from camera or image gallery"

            "tool_ocr" -> if (isHi) "छवि से टेक्स्ट (OCR)" else "Image to Text (OCR)"
            "tool_ocr_desc" -> if (isHi) "दस्तावेज़ की छवि से टेक्स्ट निकालें" else "Extract readable text from documents and photos"

            "tool_passport_photo" -> if (isHi) "पासपोर्ट फोटो मेकर" else "Passport Photo Maker"
            "tool_passport_photo_desc" -> if (isHi) "मानक आकार और पृष्ठभूमि के साथ पासपोर्ट फोटो बनाएं" else "Crop and frame standard 35x45mm passport photos"

            "tool_blank_pdf" -> if (isHi) "खाली पेज पीडीएफ मेकर" else "Blank PDF Creator"
            "tool_blank_pdf_desc" -> if (isHi) "लाइनदार, ग्रिड या सादा खाली पीडीएफ बनाएं" else "Generate lined, grid, or blank notebook PDF pages"

            else -> key
        }
    }
}
