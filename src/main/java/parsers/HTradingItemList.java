package parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class HTradingItemList {
    public int firstUserId, secondUserId;
    public List<HTradingItem> firstUserItems;
    public List<HTradingItem> secondUserItems;
    public int firstUserNumItems, secondUserNumItems;
    public int firstUserNumCredits, secondUserNumCredits;

//    {in:TradingItemList}
//      {i:29533033} first_user_id
//      {i:1} first_user_num
//          {i:265135909} item_id
//          {s:"i"} item_type
//          {i:265135909} room_item_id
//          {i:4239} item_type_id
//          {i:1} category
//          {b:true} stuff_data
//          {i:0} stuff_data
//          {s:""} stuff_data
//          {i:3} creation_day
//          {i:4} creation_month
//          {i:2023} creation_year
//          {i:1} extra
//       {i:0} first_user_num_credits
//       {i:91020171} second_user_id
//       {i:1} second_user_num
//          {i:-1674866875} item_id
//          {s:"s"} item_type
//          {i:1674866875} room_item_id
//          {i:9074} item_type_id
//          {i:1} category
//          {b:true} stuff_data
//          {i:7} stuff_data
//          {i:0} stuff_data
//          {s:""} stuff_data
//          {i:1} stuff_data
//          {i:28} creation_day
//          {i:3} creation_month
//          {i:2023} creation_year
//          {i:0} extra
//      {i:1}
//      {i:0}

    // Thanks WiredSpast
    public HTradingItemList(HPacket packet) {
        packet.resetReadIndex();

        firstUserId = packet.readInteger();
        firstUserItems = new ArrayList<>();
        int firstUserItemCount = packet.readInteger();
        for(int i = 0; i < firstUserItemCount; i++) {
            firstUserItems.add(new HTradingItem(packet));
        }
        firstUserNumItems = packet.readInteger();
        firstUserNumCredits = packet.readInteger();

        secondUserId = packet.readInteger();
        secondUserItems = new ArrayList<>();
        int secondUserItemCount = packet.readInteger();
        for (int i = 0; i < secondUserItemCount; i++) {
            secondUserItems.add(new HTradingItem(packet));
        }
        secondUserNumItems = packet.readInteger();
        secondUserNumCredits = packet.readInteger();
    }

    public int getFirstUserId() {
        return firstUserId;
    }

    public int getSecondUserId() {
        return secondUserId;
    }

    public List<HTradingItem> getFirstUserItems() {
        return firstUserItems;
    }

    public List<HTradingItem> getSecondUserItems() {
        return secondUserItems;
    }

    public int getFirstUserNumItems() {
        return firstUserNumItems;
    }

    public int getSecondUserNumItems() {
        return secondUserNumItems;
    }

    public int getFirstUserNumCredits() {
        return firstUserNumCredits;
    }

    public int getSecondUserNumCredits() {
        return secondUserNumCredits;
    }
}
