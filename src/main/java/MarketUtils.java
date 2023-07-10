import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
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
        Version = "2.1",
        Author = "Thauan"
)

public class MarketUtils extends ExtensionForm {
    public static MarketUtils RUNNING_INSTANCE;
    public CheckBox enabledOnTrade;
    public ComboBox<String> comboAddItem;
    public TextField searchItem;
    public CheckBox enabledOnDoubleClick;
    public Button addItemButton;
    public ListView<String> listOfSearchingItems;
    public CheckBox enabledSearchRoom;
    public Button clearSearchingItemList;
    public Label labelInfo;
    TreeMap<Integer, String> typeIdToNameFloor = new TreeMap<>();
    TreeMap<Integer, String> typeIdToNameWall = new TreeMap<>();
    TreeMap<Integer, Integer> furniIdToTypeId = new TreeMap<>();
    TreeMap<Integer, Integer> wallIdToTypeId = new TreeMap<>();
    List<Integer> tradedItemsChecked = new ArrayList<>();
    List<Integer> searchedItemsChecked = new ArrayList<>();
    TreeMap<Integer, String> searchItemRoomTypeIds = new TreeMap<>();
    public String host;
    public JSONArray wallJson;
    public JSONArray floorJson;
    public GAsync gAsync;
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
                Platform.runLater(() -> {
                    labelInfo.setText("There was a error, the extension will be disabled.");
                    labelInfo.setTextFill(Color.RED);
                });
                listOfSearchingItems.setDisable(true);
                comboAddItem.setDisable(true);
                searchItem.setDisable(true);
                enabledSearchRoom.setDisable(true);
                enabledOnTrade.setDisable(true);
                enabledOnDoubleClick.setDisable(true);
                System.out.println(e);
            }
        }).start();
        gAsync = new GAsync(this);

        searchItem.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(newValue);
            comboAddItem.getItems().clear();
            floorJson.forEach(o -> {
                JSONObject item = (JSONObject) o;
                try {
                    String itemName = item.get("name").toString();
                    if(itemName.toLowerCase().contains(newValue.toLowerCase()) && !comboAddItem.getItems().contains(itemName))
                        comboAddItem.getItems().add(itemName + " - (F)");
                }catch (JSONException ignored) {}
            });
            wallJson.forEach(o -> {
                JSONObject item = (JSONObject) o;
                try {
                    String itemName = item.get("name").toString();
                    if(itemName.toLowerCase().contains(newValue.toLowerCase()) && !comboAddItem.getItems().contains(itemName))
                        comboAddItem.getItems().add(itemName + " - (W)");
                }catch (JSONException ignored) {}
            });
        });

        clearSearchingItemList.setOnAction(event -> {
            searchedItemsChecked.clear();
            searchItemRoomTypeIds.clear();
            listOfSearchingItems.getItems().clear();
            Platform.runLater(() -> {
                labelInfo.setText("List of Searching Items cleared.");
                labelInfo.setTextFill(Color.GREEN);
            });
        });

        addItemButton.setOnAction(event -> {
            if(comboAddItem.getSelectionModel().isEmpty()) {
                Platform.runLater(() -> {
                    labelInfo.setText("Please select a item in the Box");
                    labelInfo.setTextFill(Color.RED);
                });
                return;
            };
            String itemName = comboAddItem.getSelectionModel().getSelectedItem();
            String finalItemName = itemName.substring(0, itemName.length() - 6);;
            System.out.println(itemName);
            if(itemName.contains("(F)")) {
            System.out.println("is F");
                floorJson.forEach(o -> {
                    JSONObject item = (JSONObject) o;
                    int typeId = item.getInt("id");
                    if(item.get("name").toString().equalsIgnoreCase(finalItemName) && !searchItemRoomTypeIds.containsKey(typeId)) {
                        searchItemRoomTypeIds.put(typeId, finalItemName);
                        listOfSearchingItems.getItems().add(finalItemName);
                        Platform.runLater(() -> {
                            labelInfo.setText("Item " + finalItemName + " added to Searching List. Don't forget to Check \"Room Item Search\"");
                            labelInfo.setTextFill(Color.GREEN);
                        });
                    }
                });
            }
            if(itemName.contains("(W)")) {
                wallJson.forEach(o -> {
                    JSONObject item = (JSONObject) o;
                    int typeId = item.getInt("id");
                    if(item.get("name").toString().equalsIgnoreCase(finalItemName) && !searchItemRoomTypeIds.containsKey(typeId)) {
                        searchItemRoomTypeIds.put(typeId, finalItemName);
                        listOfSearchingItems.getItems().add(finalItemName);
                        Platform.runLater(() -> {
                            labelInfo.setText("Item " + finalItemName + " added to Searching List.");
                            labelInfo.setTextFill(Color.GREEN);
                        });
                    }
                });
            }
        });
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

        intercept(HMessage.Direction.TOSERVER, "UseWallItem", hMessage -> {
            HPacket hPacket = hMessage.getPacket();
            int itemId = hPacket.readInteger();
            System.out.println("WIT " + wallIdToTypeId);
                if(enabledOnDoubleClick.isSelected()) {
                    if(wallIdToTypeId.containsKey(itemId)) {
                    new Thread(() -> {
                        int itemType = wallIdToTypeId.get(itemId);
                        String itemName = typeIdToNameWall.get(itemType);
                        callItemAverage(itemType, 2);
                        HPacket packet = gAsync.awaitPacket(new AwaitingPacket("MarketplaceItemStats", HMessage.Direction.TOCLIENT, 1000));
                        int value = packet.readInteger();
                        sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: The item " + itemName + " average in marketplace is " + value +  "c !\"}{i:0}{i:2}{i:0}{i:-1}"));
                    }).start();
                    hMessage.setBlocked(true);
                }
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
                        sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: The item " + itemName + " average in marketplace is " + value +  "c !\"}{i:0}{i:2}{i:0}{i:-1}"));
                    }).start();
                    hMessage.setBlocked(true);
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
            }catch (Exception e) {
                System.out.println(e);
            }

        });

        intercept(HMessage.Direction.TOCLIENT, "GetGuestRoomResult", hMessage -> {
            if(enabledSearchRoom.isSelected()) {
                for (Map.Entry<Integer, Integer> entry : furniIdToTypeId.entrySet()) {
                    isItemInRoom(entry.getValue());
                }
                for (Map.Entry<Integer, Integer> entry : wallIdToTypeId.entrySet()) {
                    isItemInRoom(entry.getValue());
                }
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "Items", hMessage -> {
            try{
                searchedItemsChecked.clear();
                wallIdToTypeId.clear();
                for(HWallItem hWallItem : HWallItem.parse(hMessage.getPacket())) {
                    if(!wallIdToTypeId.containsKey(hWallItem.getId())){
                        wallIdToTypeId.put(hWallItem.getId(), hWallItem.getTypeId());
                    }
                }
            }catch (Exception e) { System.out.println(e); }
        });

        intercept(HMessage.Direction.TOCLIENT, "Objects", hMessage -> {
            try{
                searchedItemsChecked.clear();
                furniIdToTypeId.clear();
                for (HFloorItem hFloorItem: HFloorItem.parse(hMessage.getPacket())){
                    if(!furniIdToTypeId.containsKey(hFloorItem.getId())){
                        furniIdToTypeId.put(hFloorItem.getId(), hFloorItem.getTypeId());
                    }

                }
            }catch (Exception e) { System.out.println(e); }
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
            sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: I couldn't identify the item!\"}{i:0}{i:2}{i:0}{i:-1}"));
            return;
        }

        if(!tradedItemsChecked.contains(itemTypeId)) {
            callItemAverage(itemTypeId, wallItem);
            tradedItemsChecked.add(itemTypeId);
            HPacket packet = gAsync.awaitPacket(new AwaitingPacket("MarketplaceItemStats", HMessage.Direction.TOCLIENT, 1000));
            int value = packet.readInteger();
            if(value == 0)
                sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: The item " + itemName + " has no average in the marketplace!\"}{i:0}{i:2}{i:0}{i:-1}"));
            else
                sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: The item " + itemName + " average in marketplace is " + value +  "c !\"}{i:0}{i:2}{i:0}{i:-1}"));
        }
    }
    public void callItemAverage (int itemType, int wallItem) {
        sendToServer(new HPacket("GetMarketplaceItemStats", HMessage.Direction.TOSERVER, wallItem, itemType));
    }

    public void getGameFurniData() throws Exception{
        String url = "https://www.habbo%s/gamedata/furnidata_json/1";
        JSONObject jsonObj = new JSONObject(IOUtils.toString(new URL(String.format(url, codeToDomainMap.get(host))).openStream(), StandardCharsets.UTF_8));
        floorJson = jsonObj.getJSONObject("roomitemtypes").getJSONArray("furnitype");
        floorJson.forEach(o -> {
            JSONObject item = (JSONObject)o;
            try {
                String itemName = item.get("name").toString();
                typeIdToNameFloor.put(item.getInt("id"), itemName);
            }catch (JSONException ignored) {}
        });

        wallJson = jsonObj.getJSONObject("wallitemtypes").getJSONArray("furnitype");
        wallJson.forEach(o -> {
                JSONObject item = (JSONObject) o;
                try {
                    String itemName = item.get("name").toString();
                    typeIdToNameWall.put(item.getInt("id"), itemName);
                }catch (JSONException ignored) {}
        });
    }

    public void isItemInRoom(Integer item) {
        if(searchItemRoomTypeIds.containsKey(item)) {
            if(!searchedItemsChecked.contains(item)) {
                searchedItemsChecked.add(item);
                sendToClient(new HPacket("{in:Chat}{i:" + habboIndex + "}{s:\"Market Utils: The item you are searching, " + searchItemRoomTypeIds.get(item) + " is in this room!\"}{i:0}{i:2}{i:0}{i:-1}"));
            }
        }
    }

    public void waitAFckingSec(int millisecActually){
        try {
            Thread.sleep(millisecActually);
        } catch (InterruptedException ignored) { }
    }

}
