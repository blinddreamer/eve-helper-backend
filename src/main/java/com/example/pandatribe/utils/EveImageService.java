package com.example.pandatribe.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating EVE Online image URLs.
 * Provides methods to generate icon and render links for EVE types.
 */
@Service
public class EveImageService {

    @Value("${eve.image.base.url:https://images.evetech.net}")
    private String baseImageUrl;

    /**
     * Generates an icon URL for an EVE type.
     *
     * @param typeId The EVE type ID
     * @param size The desired image size (32, 64, 128, 256, 512)
     * @return Full URL to the type's icon image
     */
    public String generateIconLink(Integer typeId, Integer size) {
        return String.format("%s/types/%d/icon?size=%d", baseImageUrl, typeId, size);
    }

    /**
     * Generates a 3D render URL for an EVE type.
     * Typically used for ships and structures.
     *
     * @param typeId The EVE type ID
     * @param size The desired image size (32, 64, 128, 256, 512)
     * @return Full URL to the type's 3D render image
     */
    public String generateRenderLink(Integer typeId, Integer size) {
        return String.format("%s/types/%d/render?size=%d", baseImageUrl, typeId, size);
    }

    /**
     * Generates a character portrait URL.
     *
     * @param characterId The character ID
     * @param size The desired image size (32, 64, 128, 256, 512)
     * @return Full URL to the character portrait
     */
    public String generateCharacterPortrait(Integer characterId, Integer size) {
        return String.format("%s/characters/%d/portrait?size=%d", baseImageUrl, characterId, size);
    }

    /**
     * Generates a corporation logo URL.
     *
     * @param corporationId The corporation ID
     * @param size The desired image size (32, 64, 128, 256)
     * @return Full URL to the corporation logo
     */
    public String generateCorporationLogo(Integer corporationId, Integer size) {
        return String.format("%s/corporations/%d/logo?size=%d", baseImageUrl, corporationId, size);
    }

    /**
     * Generates an alliance logo URL.
     *
     * @param allianceId The alliance ID
     * @param size The desired image size (32, 64, 128, 256)
     * @return Full URL to the alliance logo
     */
    public String generateAllianceLogo(Integer allianceId, Integer size) {
        return String.format("%s/alliances/%d/logo?size=%d", baseImageUrl, allianceId, size);
    }
}
