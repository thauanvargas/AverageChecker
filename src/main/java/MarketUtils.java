import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.scene.control.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import parsers.HTradingItem;
import parsers.HTradingItemList;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ExtensionInfo(
        Title = "Market Utils",
        Description = "Market Utils based on Rocawear's",
        Version = "1.0",
        Author = "Thauan"
)

public class MarketUtils extends ExtensionForm {
    public static MarketUtils RUNNING_INSTANCE;
    public CheckBox enabledOnTrade;
    TreeMap<Integer, String> typeIdToNameFloor = new TreeMap<>();
    TreeMap<Integer, String> typeIdToNameWall = new TreeMap<>();
    TreeMap<Integer, Integer> furniIdToTypeId = new TreeMap<>();

    List<Integer> tradedItemsChecked = new ArrayList<>();
    public String host;
    public GAsync gAsync;
    public CheckBox enabledOnDoubleClick;
    public int habboIndex = -1;
    public int habboId;
    private static final TreeMap<String, String> codeToDomainMap = new TreeMap<>();
    static {
        codeToDomainMap.put("br", ".com.br");
        codeToDomainMap.put("de", ".de");
        codeToDomainMap.put("es", ".es");
        codeToDomainMap.put("fi", ".fi");
        codeToDomainMap.put("fr", ".fr");
        codeToDomainMap.put("it", ".it");
        codeToDomainMap.put("nl", ".nl");
        codeToDomainMap.put("tr", ".com.tr");
        codeToDomainMap.put("us", ".com");
    }

    @Override
    protected void onStartConnection() {
        System.out.println("Market Utils started it's connection!");
        new Thread(() -> {
            System.out.println("Getting game data...");
            try {
                getGameFurniData();
                System.out.println("Game data retrieved!");
            } catch (Exception e) {
                System.out.println("There was a error, the extension will be disabled.");
                enabledOnTrade.setDisable(true);
                enabledOnDoubleClick.setDisable(true);
                System.out.println(e);
            }
        }).start();
        gAsync = new GAsync(this);
    }

    @Override
    protected void onShow() {
        new Thread(() -> {
            sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
            sendToServer(new HPacket("AvatarExpression", HMessage.Direction.TOSERVER, 0));
            sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
        }).start();
    }

    @Override
    protected void initExtension() {
        RUNNING_INSTANCE = this;

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);   // Thanks Julianty for making it usable in all hotels.
        });

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            habboId = hMessage.getPacket().readInteger();
        });

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            try {
                HEntity[] roomUsersList = HEntity.parse(hMessage.getPacket());
                for (HEntity hEntity: roomUsersList){
                    if(hEntity.getId() == habboId){
                        habboIndex = hEntity.getIndex();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        intercept(HMessage.Direction.TOCLIENT, "Expression", hMessage -> {
            if(primaryStage.isShowing() && habboIndex == -1){
                habboIndex = hMessage.getPacket().readInteger();
            }
        });

        intercept(HMessage.Direction.TOSERVER, "UseFurniture", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            int itemId = hPacket.readInteger();
            System.out.println(typeIdToNameFloor);
            if(furniIdToTypeId.containsKey(itemId)) {
                if(enabledOnDoubleClick.isSelected()) {
                    new Thread(() -> {
                        int itemType = furniIdToTypeId.get(itemId);
                        String itemName = typeIdToNameFloor.get(itemType);
                        callItemAverage(itemType, 1);
                        HPacket packet = gAsync.awaitPacket(new AwaitingPacket("MarketplaceItemStats", HMessage.Direction.TOCLIENT, 1000));
                        int value = packet.readInteger();
                        sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"AverageChecker: The item " + itemName + " average in marketplace is " + value +  "c !\"}{i:0}{i:2}{i:0}{i:-1}"));
                    }).start();
                }
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "TradingItemList", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            HTradingItemList tradingItemList = new HTradingItemList(hPacket);
            try {
                for(HTradingItem tradingItem : tradingItemList.getFirstUserItems()) {
                    if(enabledOnTrade.isSelected() && typeIdToNameFloor.containsKey(tradingItem.getItemTypeId()) || typeIdToNameWall.containsKey(tradingItem.getItemTypeId())) {
                        new Thread(() -> {
                            checkTradingItems(tradingItem);
                        }).start();
                    }
                }
                for(HTradingItem tradingItem : tradingItemList.getSecondUserItems()) {
                    if(enabledOnTrade.isSelected() && typeIdToNameFloor.containsKey(tradingItem.getItemTypeId()) || typeIdToNameWall.containsKey(tradingItem.getItemTypeId())) {
                        new Thread(() -> {
                            checkTradingItems(tradingItem);
                        }).start();
                    }
                }
            }catch (Exception ignored) {}

        });

        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            try{
                for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                    if(!furniIdToTypeId.containsKey(hFloorItem.getId())){
                        furniIdToTypeId.put(hFloorItem.getId(), hFloorItem.getTypeId());
                    }
                }
            }catch (Exception e) { System.out.println("Exception here!"); }
        });

        intercept(HMessage.Direction.TOCLIENT, "TradingClose", hMessage -> {
            tradedItemsChecked.clear();
        });
    }

    public void checkTradingItems(HTradingItem tradingItem) {
        String itemName = "";
        int itemTypeId = tradingItem.getItemTypeId();
        int wallItem = 1;
        if(Objects.equals(tradingItem.getItemType().toLowerCase(), "i")) {
            itemName = typeIdToNameWall.get(itemTypeId);
            wallItem = 2;
        }
        else if(Objects.equals(tradingItem.getItemType().toLowerCase(), "s")){
            itemName = typeIdToNameFloor.get(itemTypeId);
        }
        else {
            sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"AverageChecker: I couldn't identify the item!\"}{i:0}{i:2}{i:0}{i:-1}"));
            return;
        }

        if(!tradedItemsChecked.contains(itemTypeId)) {
            callItemAverage(itemTypeId, wallItem);
            tradedItemsChecked.add(itemTypeId);
            HPacket packet = gAsync.awaitPacket(new AwaitingPacket("MarketplaceItemStats", HMessage.Direction.TOCLIENT, 1000));
            int value = -1;
            if(wallItem == 2) {
                for (int i = 0; i < 6; i++) {
                    if(i == 4)
                        value = packet.readInteger();
                    else
                        packet.readInteger();
                }
            }else {
                value = packet.readInteger();
            }
            if(value == 0)
                sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"AverageChecker: The item " + itemName + " has no average in the marketplace!\"}{i:0}{i:2}{i:0}{i:-1}"));
            else
                sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"AverageChecker: The item " + itemName + " average in marketplace is " + value +  "c !\"}{i:0}{i:2}{i:0}{i:-1}"));
        }
    }
    public void callItemAverage (int itemType, int wallItem) {
        sendToServer(new HPacket("GetMarketplaceItemStats", HMessage.Direction.TOSERVER, wallItem, itemType));
    }

    public void getGameFurniData() throws Exception{
        String url = "https://www.habbo%s/gamedata/furnidata_json/1";
        JSONObject jsonObj = new JSONObject(IOUtils.toString(new URL(String.format(url, codeToDomainMap.get(host))).openStream(), StandardCharsets.UTF_8));
        JSONArray floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
        floorJson.forEach(o -> {
            JSONObject item = (JSONObject)o;
            try {
                String itemName = item.get("name").toString();
                typeIdToNameFloor.put(item.getInt("id"), itemName);
            }catch (JSONException ignored) {}
        });

        JSONArray wallJson = jsonObj.getJSONObject("wallitemtypes").getJSONArray("furnitype");
        wallJson.forEach(o -> {
                JSONObject item = (JSONObject)o;
                try {
                    String itemName = item.get("name").toString();
                    typeIdToNameWall.put(item.getInt("id"), itemName);
                }catch (JSONException ignored) {}
        });
    }

}
