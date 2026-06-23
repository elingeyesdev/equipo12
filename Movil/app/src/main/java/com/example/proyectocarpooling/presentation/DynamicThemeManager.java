package com.example.proyectocarpooling.presentation;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public final class DynamicThemeManager {

    public static void applyTheme(Activity activity) {
        if (activity == null) return;
        
        CarPoolingApplication app = (CarPoolingApplication) activity.getApplication();
        SessionManager session = app.getSessionManager();
        
        boolean isDarkMode = (activity.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        String primaryHex = isDarkMode ? session.getThemePrimaryDark() : session.getThemePrimaryLight();
        String secondaryHex = isDarkMode ? session.getThemeSecondaryDark() : session.getThemeSecondaryLight();
        String textHex = isDarkMode ? session.getThemeTextDark() : session.getThemeTextLight();
        String bgHex = isDarkMode ? session.getThemeBgDark() : session.getThemeBgLight();
        String cardHex = isDarkMode ? session.getThemeCardDark() : session.getThemeCardLight();
        String borderHex = isDarkMode ? session.getThemeBorderDark() : session.getThemeBorderLight();
        
        try {
            // Tint view hierarchy with the selected primary color
            View root = activity.findViewById(android.R.id.content);
            if (root != null) {
                tintViewHierarchy(root, primaryHex, secondaryHex, textHex, bgHex, cardHex, borderHex, isDarkMode);
            }
        } catch (Exception e) {
            android.util.Log.e("DynamicTheme", "Error al aplicar el tema dinámico: " + e.getMessage(), e);
        }
    }

    private static void tintViewHierarchy(View view, String primaryHex, String secondaryHex, String textHex,
                                          String bgHex, String cardHex, String borderHex, boolean isDarkMode) {
        if (view == null) return;

        try {
            int primaryColor = Color.parseColor(primaryHex);
            int textColor = Color.parseColor(textHex);
            int secColor = Color.parseColor(secondaryHex);
            int buttonColor = primaryColor;
            
            int panelColor = Color.parseColor(cardHex);
            int navColor = Color.parseColor(bgHex);
            
            int buttonTextColor = contrastFor(buttonColor);

            if ("auth_on_hero".equals(view.getTag()) && view instanceof TextView) {
                ((TextView) view).setTextColor(Color.WHITE);
            }
            // 1. Tint standard and Material Buttons (excluding back button in account overview to keep it transparent, and handling toggle groups natively)
            if (view instanceof Button && !(view instanceof CompoundButton) && view.getId() != com.example.proyectocarpooling.R.id.accountBackButton) {
                if (view.getParent() instanceof com.google.android.material.button.MaterialButtonToggleGroup) {
                    com.google.android.material.button.MaterialButton toggleBtn = (com.google.android.material.button.MaterialButton) view;
                    int[][] states = new int[][] {
                        new int[] { android.R.attr.state_checked },
                        new int[] { -android.R.attr.state_checked }
                    };
                    ColorStateList bgStates = new ColorStateList(states, new int[] { buttonColor, Color.TRANSPARENT });
                    ColorStateList textStates = new ColorStateList(states, new int[] { buttonTextColor, buttonColor });
                    ColorStateList strokeStates = new ColorStateList(states, new int[] { buttonColor, buttonColor });

                    toggleBtn.setBackgroundTintList(bgStates);
                    toggleBtn.setTextColor(textStates);
                    toggleBtn.setStrokeColor(strokeStates);
                } else {
                    Button btn = (Button) view;
                    btn.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
                    btn.setTextColor(buttonTextColor);
                }
            }
            // 2. Tint Floating Action Buttons (FABs) with Secondary
            else if (view instanceof FloatingActionButton) {
                FloatingActionButton fab = (FloatingActionButton) view;
                fab.setBackgroundTintList(ColorStateList.valueOf(secColor));
            }
            // 3. Tint CompoundButtons (Switches, CheckBoxes, RadioButtons) with Secondary
            else if (view instanceof CompoundButton) {
                CompoundButton compoundButton = (CompoundButton) view;
                compoundButton.setButtonTintList(ColorStateList.valueOf(secColor));
                if (view.getId() == com.example.proyectocarpooling.R.id.registerHasVehicleCheckbox ||
                        view.getId() == com.example.proyectocarpooling.R.id.profileHasVehicleCheckbox) {
                    compoundButton.setBackgroundResource(com.example.proyectocarpooling.R.drawable.bg_info_field);
                    compoundButton.setBackgroundTintList(null);
                    compoundButton.setTextColor(textColor);
                }
            }
            // 4. Tint explicitly tinted ImageViews
            else if (view instanceof ImageView) {
                ImageView imageView = (ImageView) view;
                if (imageView.getImageTintList() != null) {
                    imageView.setImageTintList(ColorStateList.valueOf(primaryColor));
                }
            }
            // 5. Tint NavigationView (Sidebar) with Secondary Background and handle Header views
            else if (view instanceof com.google.android.material.navigation.NavigationView) {
                com.google.android.material.navigation.NavigationView navView = (com.google.android.material.navigation.NavigationView) view;
                
                navView.setBackgroundColor(navColor);
                
                boolean isSecondaryDark = androidx.core.graphics.ColorUtils.calculateLuminance(navColor) < 0.5;
                int contrastColor = isSecondaryDark ? Color.WHITE : Color.BLACK;
                int inactiveColor = isSecondaryDark ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#888888");
                
                int[][] states = new int[][] {
                    new int[] { android.R.attr.state_checked },
                    new int[] { -android.R.attr.state_checked }
                };
                int[] colors = new int[] { primaryColor, inactiveColor };
                int[] textColors = new int[] { primaryColor, contrastColor };
                
                navView.setItemIconTintList(new ColorStateList(states, colors));
                navView.setItemTextColor(new ColorStateList(states, textColors));

                // Tint any active headers inside the NavigationView!
                int headerCount = navView.getHeaderCount();
                for (int h = 0; h < headerCount; h++) {
                    View headerView = navView.getHeaderView(h);
                    if (headerView != null) {
                        tintViewHierarchy(headerView, primaryHex, secondaryHex, textHex, bgHex, cardHex, borderHex, isDarkMode);
                    }
                }
            }

            // 6. Tint Sidebar Header and Profile Detail Header Backgrounds with Primary Color (different from sidebar to create contrast!)
            if (view.getId() == com.example.proyectocarpooling.R.id.sidebarHeaderLayout || 
                view.getId() == com.example.proyectocarpooling.R.id.accountHeaderLayout) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(primaryColor);
                } else {
                    view.setBackgroundColor(primaryColor);
                }
            }

            // 7. High-Contrast Text Overrides inside colored Headers & Badges
            boolean isSecondaryDark = androidx.core.graphics.ColorUtils.calculateLuminance(secColor) < 0.5;
            boolean isPrimaryDark = androidx.core.graphics.ColorUtils.calculateLuminance(primaryColor) < 0.5;

            // Enclosing parent checks (like children of drawerReservationInfo card)
            boolean isInsideReservationInfo = false;
            if (view.getParent() instanceof View) {
                View parent = (View) view.getParent();
                if (parent.getId() == com.example.proyectocarpooling.R.id.drawerReservationInfo) {
                    isInsideReservationInfo = true;
                }
            }

            // Initials avatar circle (drawn with secondary color matching sidebar, creating contrast against primary header!)
            if (view.getId() == com.example.proyectocarpooling.R.id.drawerUserInitials || 
                view.getId() == com.example.proyectocarpooling.R.id.accountUserInitials) {
                TextView tv = (TextView) view;
                tv.setTextColor(isSecondaryDark ? Color.WHITE : Color.BLACK);
                
                if (view.getParent() instanceof View) {
                    View parent = (View) view.getParent();
                    if (parent.getBackground() != null) {
                        parent.getBackground().mutate().setTint(secColor);
                    }
                }
            }
            // Role Badge (drawn with secondary color matching sidebar, creating contrast against primary header)
            else if (view.getId() == com.example.proyectocarpooling.R.id.drawerUserRole) {
                TextView tv = (TextView) view;
                tv.setTextColor(isSecondaryDark ? Color.WHITE : Color.BLACK);
                
                if (view.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent.getChildCount() > 0) {
                        View badgeBg = parent.getChildAt(0);
                        if (badgeBg != null && badgeBg.getBackground() != null) {
                            badgeBg.getBackground().mutate().setTint(secColor);
                        }
                    }
                }
            }
            // Primary Name Text in Headers (Sidebar Header & Profile Header) - colored based on Primary background
            else if (view.getId() == com.example.proyectocarpooling.R.id.drawerUserTitle || 
                     view.getId() == com.example.proyectocarpooling.R.id.accountNameValue) {
                TextView tv = (TextView) view;
                tv.setTextColor(isPrimaryDark ? Color.WHITE : Color.BLACK);
            }
            // Secondary Muted Subtitles in Headers - colored based on Primary background
            else if (view.getId() == com.example.proyectocarpooling.R.id.drawerUserEmail || 
                     view.getId() == com.example.proyectocarpooling.R.id.drawerUserRating || 
                     view.getId() == com.example.proyectocarpooling.R.id.accountEmailValue) {
                TextView tv = (TextView) view;
                tv.setTextColor(isPrimaryDark ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#66000000"));
            }
            // Back Arrow Button in Profile Header (transparent, just text) - colored based on Primary background
            else if (view.getId() == com.example.proyectocarpooling.R.id.accountBackButton) {
                Button btn = (Button) view;
                btn.setTextColor(isPrimaryDark ? Color.WHITE : Color.BLACK);
            }
            // Reservation Badge parent container (drawn with secondary color matching sidebar/contrast header)
            else if (view.getId() == com.example.proyectocarpooling.R.id.drawerReservationInfo) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(secColor);
                }
            }
            // Text views inside the reservation info badge
            else if (isInsideReservationInfo && view instanceof TextView) {
                TextView tv = (TextView) view;
                boolean isBadgeDark = androidx.core.graphics.ColorUtils.calculateLuminance(secColor) < 0.5;
                if (view.getId() == com.example.proyectocarpooling.R.id.drawerReservationHint) {
                    tv.setTextColor(isBadgeDark ? Color.parseColor("#88FFFFFF") : Color.parseColor("#88000000"));
                } else {
                    tv.setTextColor(isBadgeDark ? Color.WHITE : Color.BLACK);
                }
            }
            // 8. General standard TextViews (excluding custom controls)
            else if (view instanceof TextView && !(view instanceof Button) && !(view instanceof EditText) && !(view instanceof CompoundButton) && !"auth_on_hero".equals(view.getTag())) {
                TextView tv = (TextView) view;
                tv.setTextColor(textColor);
            }
            
            // 9. Tint Bottom Bar (controlPanel) background with Primary (matching sidebar)
            if (view.getId() == com.example.proyectocarpooling.R.id.controlPanel) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(panelColor);
                } else {
                    view.setBackgroundColor(panelColor);
                }
            }

            if (view.getId() == com.example.proyectocarpooling.R.id.menuToggleButton) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(panelColor);
                }
            }

            if (view.getId() == com.example.proyectocarpooling.R.id.authHeroLayout) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(primaryColor);
                } else {
                    view.setBackgroundColor(primaryColor);
                }
            }

            // 10. Tint origin and destination dots on map query panel with Primary Color
            if (view.getId() == com.example.proyectocarpooling.R.id.originDot || 
                view.getId() == com.example.proyectocarpooling.R.id.destinationDot) {
                if (view.getBackground() != null) {
                    view.getBackground().mutate().setTint(primaryColor);
                } else {
                    view.setBackgroundColor(primaryColor);
                }
            }
        } catch (Exception e) {
            // Ignore color parsing errors for individual views
        }

        // Recurse for view groups
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintViewHierarchy(group.getChildAt(i), primaryHex, secondaryHex, textHex, bgHex, cardHex, borderHex, isDarkMode);
            }
        }
    }

    private static int contrastFor(int color) {
        return androidx.core.graphics.ColorUtils.calculateLuminance(color) < 0.48 ? Color.WHITE : Color.parseColor("#24302b");
    }
}
