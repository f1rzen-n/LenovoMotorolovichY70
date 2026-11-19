package com.moto.resolutionmaster;

import android.graphics.Point;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ResolutionTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (DisplayManagerHelper.isFHD(this)) {
            // Switch to Native 1440p
            DisplayManagerHelper.resetResolution(this);
        } else {
            // Switch to FHD+ 1080p
            DisplayManagerHelper.setResolution(this, 
                DisplayManagerHelper.FHD_WIDTH, 
                DisplayManagerHelper.FHD_HEIGHT, 
                DisplayManagerHelper.FHD_DENSITY);
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        Point p = DisplayManagerHelper.getCurrentResolution(this);
        int minRes = Math.min(p.x, p.y);

        if (minRes <= 1080) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel(getString(R.string.tile_fhd_label));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.tile_fhd_sub));
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_qhd_label));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(R.string.tile_qhd_sub));
            }
        }
        tile.updateTile();
    }
}
