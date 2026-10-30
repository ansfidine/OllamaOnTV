package io.ollamaontv;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.Locale;

final class LogoKit {
    private LogoKit() {}

    static Drawable logo(Context context, String model) {
        String name = model == null ? "" : model.toLowerCase(Locale.ROOT);
        int res = 0;
        if (name.startsWith("deepseek")) res = R.drawable.logo_deepseek;
        else if (name.startsWith("mistral")) res = R.drawable.logo_mistral;
        else if (name.startsWith("nemotron")) res = R.drawable.logo_nvidia;
        else if (name.startsWith("kimi") || name.startsWith("moonshot")) res = R.drawable.logo_kimi;
        else if (name.startsWith("glm") || name.startsWith("zhipu")) res = R.drawable.logo_glm;
        else if (name.startsWith("gemma")) res = R.drawable.logo_gemma;
        else if (name.startsWith("minimax")) res = R.drawable.logo_minimax;
        else if (name.startsWith("gpt") || name.startsWith("oss")) res = R.drawable.logo_openai;
        else if (name.startsWith("muse")) res = R.drawable.logo_meta;
        return res == 0 ? null : context.getDrawable(res);
    }
}
