package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage {
    ENGLISH,
    HINDI
}

object LocaleManager {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("app_locale_prefs", Context.MODE_PRIVATE)
        val savedLang = prefs?.getString("lang_choice", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        currentLanguage = try {
            AppLanguage.valueOf(savedLang)
        } catch (e: Exception) {
            AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
        prefs?.edit()?.putString("lang_choice", lang.name)?.apply()
    }

    fun toggleLanguage() {
        val next = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun isHindi() = currentLanguage == AppLanguage.HINDI
}

object Strings {
    fun get(key: String): String {
        val isHi = LocaleManager.isHindi()
        return when (key) {
            "app_name" -> if (isHi) "ऑलटूल्स मास्टर" else "AllToolsMaster"
            "app_tagline" -> if (isHi) "ऑल-इन-वन पीडीएफ, फोटो और स्कैनर" else "All-in-One PDF, Photo & Scanner"
            "official_website" -> if (isHi) "ऑफिशियल वेबसाइट" else "Official Website"
            "whatsapp_support" -> if (isHi) "व्हाट्सएप सहायता" else "WhatsApp Support"
            "website_url" -> "https://www.alltoolspro.in/"
            "whatsapp_number" -> "+91 6202292319"
            "search_tools_placeholder" -> if (isHi) "टूल्स खोजें (उदा. कंप्रेस, स्कैनर, क्यूआर)..." else "Search tools (e.g. Compress, Scanner, QR)..."
            "popular_tools" -> if (isHi) "लोकप्रिय टूल्स" else "Popular Tools"
            "categories" -> if (isHi) "श्रेणियां" else "Categories"
            "recent_files" -> if (isHi) "हालिया फाइलें" else "Recent Files"
            "favorites" -> if (isHi) "पसंदीदा टूल्स" else "Favorites"
            "no_favorites" -> if (isHi) "कोई पसंदीदा टूल नहीं जोड़ा गया" else "No favorite tools added yet"
            "no_recent_files" -> if (isHi) "अभी तक कोई हाल की फ़ाइल नहीं" else "No recent files yet"
            "open_a_tool" -> if (isHi) "कोई टूल खोलें" else "Open a tool"
            "scan_a_document" -> if (isHi) "दस्तावेज़ स्कैन करें" else "Scan a document"
            "files_empty_helper" -> if (isHi) "आपके द्वारा बनाए या संपादित किए गए दस्तावेज़ यहां दिखाई देंगे" else "Files you create, compress or scan will appear here"
            
            // Header & Navigation
            "back" -> if (isHi) "वापस" else "Back"
            "home" -> if (isHi) "होम" else "Home"
            "cd_back" -> if (isHi) "पीछे जाएं" else "Go back"
            "cd_home" -> if (isHi) "होम पर जाएं" else "Go to Home"

            // Permissions
            "camera_permission_needed" -> if (isHi) "दस्तावेज़ स्कैन करने के लिए कैमरा अनुमति ज़रूरी है" else "Camera permission is required to scan documents"
            "open_settings" -> if (isHi) "सेटिंग्स खोलें" else "Open Settings"
            "camera_open_failed" -> if (isHi) "कैमरा नहीं खुल पाया। कृपया कैमरा अनुमति दें।" else "Could not open camera. Please allow camera permission."

            // Favorites feedback
            "added_to_favorites" -> if (isHi) "पसंदीदा में जोड़ा गया" else "Added to Favorites"
            "removed_from_favorites" -> if (isHi) "पसंदीदा से हटाया गया" else "Removed from Favorites"
            "undo" -> if (isHi) "पूर्ववत करें" else "Undo"

            // Cache
            "clear_cache" -> if (isHi) "अस्थायी कैश साफ़ करें" else "Clear Temporary Cache"
            "cache_cleared" -> if (isHi) "कैश साफ़ हो गया। %s जगह खाली हुई।" else "Cache cleared. %s freed."
            "confirm_clear_cache" -> if (isHi) "क्या आप वाकई %s अस्थायी कैश हटाना चाहते हैं?" else "Are you sure you want to delete %s of temporary cache?"
            "calculating" -> if (isHi) "गणना जारी है..." else "Calculating..."

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
            "confirm" -> if (isHi) "पुष्टि करें" else "Confirm"
            "processing" -> if (isHi) "प्रक्रिया जारी है..." else "Processing..."
            "success" -> if (isHi) "सफल!" else "Success!"
            "error" -> if (isHi) "त्रुटि" else "Error"
            "original_size" -> if (isHi) "मूल आकार" else "Original Size"
            "output_size" -> if (isHi) "आउटपुट आकार" else "Output Size"
            "target_size" -> if (isHi) "लक्षित आकार" else "Target Size"
            "compression_ratio" -> if (isHi) "आकार में कमी" else "Reduction"
            "privacy_notice" -> if (isHi) "100% ऑन-डिवाइस सुरक्षित। आपकी फाइलें कभी सर्वर पर नहीं भेजी जाती हैं।" else "100% On-Device & Private. Files are processed locally and never uploaded."

            // Settings
            "theme" -> if (isHi) "थीम" else "Theme"
            "theme_system" -> if (isHi) "सिस्टम डिफ़ॉल्ट" else "System Default"
            "theme_light" -> if (isHi) "लाइट थीम" else "Light Theme"
            "theme_dark" -> if (isHi) "डार्क थीम" else "Dark Theme"
            "rate_app" -> if (isHi) "ऐप को रेट करें" else "Rate the app"
            "share_app" -> if (isHi) "ऐप शेयर करें" else "Share the app"
            "privacy_policy" -> if (isHi) "गोपनीयता नीति" else "Privacy Policy"
            "send_feedback" -> if (isHi) "प्रतिक्रिया भेजें" else "Send Feedback"
            "about" -> if (isHi) "ऐप के बारे में" else "About"
            "developer" -> if (isHi) "डेवलपर" else "Developer"
            "version" -> if (isHi) "वर्जन" else "Version"

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

            // Scanner Modes
            "mode_docs" -> if (isHi) "दस्तावेज़" else "Docs"
            "mode_id_card" -> if (isHi) "आईडी कार्ड" else "ID Card"
            "mode_book" -> if (isHi) "किताब" else "Book"
            "mode_to_text" -> if (isHi) "टेक्स्ट में" else "To Text"
            "mode_qr_code" -> if (isHi) "क्यूआर कोड" else "QR Code"

            // Camera Overlays & Controls
            "auto_capture" -> if (isHi) "ऑटो कैप्चर" else "Auto Capture"
            "manual_capture" -> if (isHi) "मैनुअल" else "Manual"
            "flash_auto" -> if (isHi) "फ्लैश ऑटो" else "Flash Auto"
            "flash_on" -> if (isHi) "फ्लैश ऑन" else "Flash On"
            "flash_off" -> if (isHi) "फ्लैश ऑफ" else "Flash Off"
            "grid_overlay" -> if (isHi) "ग्रिड" else "Grid"
            "hd_mode" -> if (isHi) "एचडी मोड" else "HD Mode"
            "scan_front_side" -> if (isHi) "कार्ड का अगला भाग स्कैन करें" else "Scan Front Side of Card"
            "scan_back_side" -> if (isHi) "कार्ड का पिछला भाग स्कैन करें" else "Scan Back Side of Card"
            "card_front" -> if (isHi) "अगला भाग" else "Front"
            "card_back" -> if (isHi) "पिछला भाग" else "Back"
            "import_files" -> if (isHi) "फाइलें आयात करें" else "Import Files"
            "capture_ready" -> if (isHi) "कैप्चर करने के लिए टैप करें" else "Tap to capture"
            "detecting_edges" -> if (isHi) "किनारों की पहचान हो रही है..." else "Detecting edges..."

            // Crop & Perspective
            "crop_title" -> if (isHi) "क्रॉप और परिप्रेक्ष्य सुधार" else "Crop & Perspective"
            "auto_detect" -> if (isHi) "ऑटो डिटेक्ट" else "Auto Detect"
            "full_page" -> if (isHi) "पूरा पेज" else "Full Page"
            "rotate_left" -> if (isHi) "बाएं घुमाएं" else "Rotate Left"
            "rotate_right" -> if (isHi) "दाएं घुमाएं" else "Rotate Right"
            "drag_corners_hint" -> if (isHi) "सटीक क्रॉप के लिए 4 कोनों को खींचें" else "Drag 4 corner handles to adjust crop"
            "next_step" -> if (isHi) "आगे बढ़ें" else "Next"
            "retake" -> if (isHi) "दोबारा लें" else "Retake"

            // Document Enhancement Filters
            "enhance_title" -> if (isHi) "दस्तावेज़ सुधार" else "Document Enhancement"
            "filter_original" -> if (isHi) "मूल" else "Original"
            "filter_lighten" -> if (isHi) "लाइटेन" else "Lighten"
            "filter_magic_color" -> if (isHi) "मैजिक कलर" else "Magic Color"
            "filter_grayscale" -> if (isHi) "ग्रेस्केल" else "Grayscale"
            "filter_bw" -> if (isHi) "ब्लैक & व्हाइट" else "Black & White"
            "filter_shadow_reduction" -> if (isHi) "छाया हटाएँ" else "Shadow Reduction"
            "hold_for_original" -> if (isHi) "मूल देखने के लिए दबाकर रखें" else "Hold for Original"
            "filter_intensity" -> if (isHi) "फ़िल्टर तीव्रता" else "Filter Intensity"
            "add_page" -> if (isHi) "पेज जोड़ें" else "Add Page"
            "reorder_pages" -> if (isHi) "पेज क्रम बदलें" else "Reorder Pages"

            // PDF Editor
            "pdf_editor_title" -> if (isHi) "पीडीएफ संपादक" else "PDF Editor"
            "add_text" -> if (isHi) "टेक्स्ट जोड़ें" else "Add Text"
            "markup_draw" -> if (isHi) "ड्राइंग / पेन" else "Draw / Pen"
            "add_signature" -> if (isHi) "हस्ताक्षर" else "Signature"
            "pen_tool" -> if (isHi) "पेन" else "Pen"
            "highlighter_tool" -> if (isHi) "हाइलाइटर" else "Highlighter"
            "eraser_tool" -> if (isHi) "इरेज़र" else "Eraser"
            "clear_all" -> if (isHi) "सभी साफ़ करें" else "Clear All"
            "draw_signature" -> if (isHi) "हस्ताक्षर बनाएं" else "Draw Signature"
            "save_signature" -> if (isHi) "हस्ताक्षर सहेजें" else "Save Signature"
            "insert_signature" -> if (isHi) "हस्ताक्षर लगाएं" else "Insert Signature"
            "export_pdf" -> if (isHi) "पीडीएफ निर्यात करें" else "Export PDF"
            "save_to_docs" -> if (isHi) "दस्तावेज़ में सहेजें" else "Save to Docs"
            "document_saved" -> if (isHi) "दस्तावेज़ सफलतापूर्वक सहेजा गया!" else "Document saved successfully!"
            "watermark_text" -> if (isHi) "वॉटरमार्क टेक्स्ट" else "Watermark Text"

            // Docs Manager (Docs Tab)
            "docs_title" -> if (isHi) "दस्तावेज़" else "Docs"
            "search_docs" -> if (isHi) "दस्तावेज़ खोजें..." else "Search documents..."
            "new_folder" -> if (isHi) "+ नया फ़ोल्डर" else "+ New Folder"
            "create_folder" -> if (isHi) "फ़ोल्डर बनाएं" else "Create Folder"
            "folder_name" -> if (isHi) "फ़ोल्डर का नाम" else "Folder Name"
            "folder_all" -> if (isHi) "सभी" else "All"
            "total_docs_count" -> if (isHi) "%d दस्तावेज़" else "%d Docs"
            "sort_date" -> if (isHi) "तिथि (नवीनतम)" else "Date (Newest)"
            "sort_name" -> if (isHi) "नाम (A-Z)" else "Name (A-Z)"
            "sort_size" -> if (isHi) "आकार (बड़ा से छोटा)" else "Size (Largest)"
            "view_grid" -> if (isHi) "ग्रिड दृश्य" else "Grid View"
            "view_list" -> if (isHi) "सूची दृश्य" else "List View"
            "select_items" -> if (isHi) "चुनें" else "Select"
            "selected_count" -> if (isHi) "%d चयनित" else "%d Selected"
            "move_to_folder" -> if (isHi) "फ़ोल्डर में ले जाएं" else "Move to Folder"
            "delete_selected" -> if (isHi) "हटाएं" else "Delete"
            "share_selected" -> if (isHi) "शेयर करें" else "Share"
            "scan_new_docs" -> if (isHi) "नया दस्तावेज़ स्कैन करें" else "Scan New Docs"
            "no_docs_found" -> if (isHi) "कोई दस्तावेज़ नहीं मिला" else "No documents found"
            "rename_document" -> if (isHi) "नाम बदलें" else "Rename Document"
            "open_pdf" -> if (isHi) "पीडीएफ खोलें" else "Open PDF"

            // OCR
            "ocr_title" -> if (isHi) "टेक्स्ट निष्कर्षण (OCR)" else "Text Extraction (OCR)"
            "extracting_text" -> if (isHi) "टेक्स्ट निकाला जा रहा है..." else "Extracting text with on-device OCR..."
            "copy_text" -> if (isHi) "कॉपी करें" else "Copy"
            "text_copied" -> if (isHi) "टेक्स्ट कॉपी हो गया!" else "Text copied to clipboard!"
            "export_txt" -> if (isHi) "TXT डाउनलोड" else "Download TXT"
            "export_pdf_ocr" -> if (isHi) "पीडीएफ डाउनलोड" else "Download PDF"
            "word_count" -> if (isHi) "शब्द: %d" else "Words: %d"
            "line_count" -> if (isHi) "पंक्तियां: %d" else "Lines: %d"
            "recognized_text" -> if (isHi) "पहचाना गया टेक्स्ट" else "Recognized Text"

            // Downloads & Feedback
            "download_successful" -> if (isHi) "✅ डाउनलोड सफल!" else "✅ Download Successful!"
            "download_to_device" -> if (isHi) "डाउनलोड करें" else "Download"
            "file_saved_downloads" -> if (isHi) "फाइल डाउनलोड्स में सुरक्षित की गई" else "File saved to Downloads"

            else -> key
        }
    }
}
