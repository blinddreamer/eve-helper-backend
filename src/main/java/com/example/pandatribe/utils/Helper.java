package com.example.pandatribe.utils;

import com.example.pandatribe.models.industry.BuildingBonus;
import com.example.pandatribe.models.industry.RigBonus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class Helper {

    private final HashMap<Integer, BuildingBonus> buildingBonuses = new HashMap<>() {{
        put(0, BuildingBonus.builder().costReduction(0).materialReduction(0).build());
        put(1, BuildingBonus.builder().costReduction(4).materialReduction(1).build());
        put(2, BuildingBonus.builder().costReduction(3).materialReduction(1).build());
        put(3, BuildingBonus.builder().costReduction(5).materialReduction(1).build());
        put(4, BuildingBonus.builder().costReduction(0).materialReduction(0).build());
        put(5, BuildingBonus.builder().costReduction(0).materialReduction(0).build());
    }};

    private final HashMap<Integer, RigBonus> rigBonuses = new HashMap<>() {{
        put(0, RigBonus.builder().materialReduction(0.0).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build());
        put(1, RigBonus.builder().materialReduction(2.0).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build());
        put(2, RigBonus.builder().materialReduction(2.4).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build());
        put(3, RigBonus.builder().materialReduction(2.0).highSecMultiplier(0.0).lowSecMultiplier(1.0).nullSecMultiplier(1.1).build());
        put(4, RigBonus.builder().materialReduction(2.4).highSecMultiplier(0.0).lowSecMultiplier(1.0).nullSecMultiplier(1.1).build());

    }};

    public String getCodes() {
        String codeVerifier = generateRandomCodeVerifier();

        // Generate the code_challenge using SHA-256
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return codeChallenge;

    }

    public String generateIconLink(Integer typeId, Integer size){
        return String.format("https://images.evetech.net/types/%d/icon?size=%d",typeId,size);
    }

    public String generateRenderLink(Integer typeId, Integer size){
        return String.format("https://images.evetech.net/types/%d/render?size=%d",typeId, size);
    }
    public BuildingBonus getBuildingBonus(Integer index){
        return buildingBonuses.get(index);
    }

    public RigBonus getRigBonus(Integer index, Integer building){
        index = building > 3 ? index + 2 : index;
        return rigBonuses.get(index);
    }
    private String generateRandomCodeVerifier() {
        byte[] randomBytes = new byte[32]; // 256 bits
        ThreadLocalRandom.current().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Handle the exception appropriately
            e.printStackTrace();
            return null;
        }
    }
}