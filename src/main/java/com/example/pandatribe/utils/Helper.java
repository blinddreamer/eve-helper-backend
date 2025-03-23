package com.example.pandatribe.utils;

import com.example.pandatribe.models.industry.BuildingBonus;
import com.example.pandatribe.models.industry.RigBonus;
import org.bitcoinj.core.Base58;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class Helper {

    private final Map<Integer, BuildingBonus> buildingBonuses = new HashMap<>(Map.ofEntries(
            Map.entry(0, BuildingBonus.builder().costReduction(0).materialReduction(0).build()),
            Map.entry(1, BuildingBonus.builder().costReduction(4).materialReduction(1).build()),
            Map.entry(2, BuildingBonus.builder().costReduction(3).materialReduction(1).build()),
            Map.entry(3, BuildingBonus.builder().costReduction(5).materialReduction(1).build()),
            Map.entry(4, BuildingBonus.builder().costReduction(0).materialReduction(0).build()),
            Map.entry(5, BuildingBonus.builder().costReduction(0).materialReduction(0).build())
    ));

    private final Map<Integer, RigBonus> rigBonuses = new HashMap<>(Map.ofEntries(
            Map.entry(0, RigBonus.builder().materialReduction(0.0).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build()),
            Map.entry(1, RigBonus.builder().materialReduction(2.0).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build()),
            Map.entry(2, RigBonus.builder().materialReduction(2.4).highSecMultiplier(1.0).lowSecMultiplier(1.9).nullSecMultiplier(2.1).build()),
            Map.entry(3, RigBonus.builder().materialReduction(2.0).highSecMultiplier(0.0).lowSecMultiplier(1.0).nullSecMultiplier(1.1).build()),
            Map.entry(4, RigBonus.builder().materialReduction(2.4).highSecMultiplier(0.0).lowSecMultiplier(1.0).nullSecMultiplier(1.1).build())
    ));

    public String generateBasicAuthToken(String clientId, String clientSecret) {
        String authValue = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(authValue.getBytes());
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

    // Compresses an Integer into a Base58 string
    public String compressInteger(Integer number) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4); // Integer = 4 bytes
        byteBuffer.putInt(number);
        return Base58.encode(byteBuffer.array()); // Base58 encoding
    }

    // Decompresses a Base58 string back into an Integer
    public Integer decompressInteger(String shortNumber) {
        byte[] bytes = Base58.decode(shortNumber);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt();
    }

    public String compressUUID(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());

        return Base58.encode(byteBuffer.array()); // Base58 is URL-safe
    }

    public UUID decompressUUID(String shortUuid) {
        byte[] bytes = Base58.decode(shortUuid);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSigBits = byteBuffer.getLong();
        long leastSigBits = byteBuffer.getLong();

        return new UUID(mostSigBits, leastSigBits);
    }
}