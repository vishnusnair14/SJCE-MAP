package com.vishnu.sjcemap.miscellaneous;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.PopupWindow;

import com.vishnu.sjcemap.R;

public class Overlay360View extends PopupWindow {
    Button backBtn;

    public Overlay360View(Context context, String gmap360ViewUrl) {
        super(context);

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <body>
                    """ + gmap360ViewUrl + """
                </body>
                </html>
                """;

        View contentView = LayoutInflater.from(context).inflate(R.layout.overlay_360_view_layout, null);
        setContentView(contentView);

        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);

        WebView webView = contentView.findViewById(R.id.gmap360View_webView);
        backBtn = contentView.findViewById(R.id.overlayBack_button);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                dismiss();
            });
        }

        contentView.setBackgroundColor(Color.parseColor("#D5D0D0"));

        webView.loadData(htmlContent, "text/html", "UTF-8");
    }
}
