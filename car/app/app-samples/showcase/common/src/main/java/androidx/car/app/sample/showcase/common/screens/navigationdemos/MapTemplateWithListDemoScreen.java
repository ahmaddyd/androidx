/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.sample.showcase.common.screens.navigationdemos;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.CarToast.LENGTH_SHORT;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapController;
import androidx.car.app.navigation.model.MapTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.navigation.routing.RoutingDemoModels;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;

/** Simple demo of how to present a map template with a list. */
public class MapTemplateWithListDemoScreen extends Screen {
    private static final int MAX_LIST_ITEMS = 100;
    private boolean mIsFavorite;

    public MapTemplateWithListDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(
                new Row.Builder()
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(() -> onClick(
                                        getCarContext().getString(R.string.parked_toast_msg))))
                        .setTitle(getCarContext().getString(R.string.parked_only_title))
                        .addText(getCarContext().getString(R.string.parked_only_text))
                        .build());
        // Some hosts may allow more items in the list than others, so create more.
        if (getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_1) {
            int listLimit =
                    Math.min(MAX_LIST_ITEMS,
                            getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                                    ConstraintManager.CONTENT_LIMIT_TYPE_LIST));

            for (int i = 2; i <= listLimit; ++i) {
                // For row text, set text variants that fit best in different screen sizes.
                String secondTextStr = getCarContext().getString(R.string.second_line_text);
                CarText secondText =
                        new CarText.Builder(
                                "================= " + secondTextStr + " ================")
                                .addVariant("--------------------- " + secondTextStr
                                        + " ----------------------")
                                .addVariant(secondTextStr)
                                .build();
                final String onClickText = getCarContext().getString(R.string.clicked_row_prefix)
                        + ": " + i;
                listBuilder.addItem(
                        new Row.Builder()
                                .setOnClickListener(() -> onClick(onClickText))
                                .setTitle(
                                        getCarContext().getString(R.string.title_prefix) + " " + i)
                                .addText(getCarContext().getString(R.string.first_line_text))
                                .addText(secondText)
                                .build());
            }
        }

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                mIsFavorite
                                                        ? R.drawable.ic_favorite_filled_white_24dp
                                                        : R.drawable.ic_favorite_white_24dp))
                                        .build())
                        .setOnClickListener(() -> {
                            mIsFavorite = !mIsFavorite;
                            CarToast.makeText(
                                            getCarContext(),
                                            mIsFavorite
                                                    ? getCarContext().getString(
                                                    R.string.favorite_toast_msg)
                                                    : getCarContext().getString(
                                                            R.string.not_favorite_toast_msg),
                                            LENGTH_SHORT)
                                    .show();
                            invalidate();
                        })
                        .build())
                .addEndHeaderAction(new Action.Builder()
                        .setOnClickListener(() -> finish())
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_close_white_24dp))
                                        .build())
                        .build())
                .setTitle(getCarContext().getString(R.string.map_template_list_demo_title))
                .build();


        MapController mapController = new MapController.Builder()
                .setMapActionStrip(RoutingDemoModels.getMapActionStrip(getCarContext()))
                .build();

        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.bug_reported_toast_msg),
                                                        CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        getCarContext(),
                                                        R.drawable.ic_bug_report_24px))
                                                .build())
                                .setFlags(Action.FLAG_IS_PERSISTENT)
                                .build())
                .build();

        MapTemplate.Builder builder = new MapTemplate.Builder()
                .setItemList(listBuilder.build())
                .setActionStrip(actionStrip)
                .setHeader(header)
                .setMapController(mapController);

        return builder.build();
    }

    private void onClick(String text) {
        CarToast.makeText(getCarContext(), text, LENGTH_LONG).show();
    }
}
