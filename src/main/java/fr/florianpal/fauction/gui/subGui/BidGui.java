package fr.florianpal.fauction.gui.subGui;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import fr.florianpal.fauction.FAuction;
import fr.florianpal.fauction.configurations.BidConfig;
import fr.florianpal.fauction.gui.AbstractGuiWithBill;
import fr.florianpal.fauction.gui.GuiInterface;
import fr.florianpal.fauction.languages.MessageKeys;
import fr.florianpal.fauction.objects.Auction;
import fr.florianpal.fauction.objects.Barrier;
import fr.florianpal.fauction.objects.Bill;
import fr.florianpal.fauction.utils.FormatUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BidGui extends AbstractGuiWithBill implements GuiInterface {

    private final BidConfig bidConfig;

    private final List<LocalDateTime> spamTest = new ArrayList<>();

    public BidGui(FAuction plugin, Player player, List<Bill> bills, int page) {
        super(plugin, player, page, bills, plugin.getConfigurationManager().getAuctionConfig());
        this.bidConfig = plugin.getConfigurationManager().getBidConfig();
        initGui(bidConfig.getNameGui(), bidConfig.getSize());
    }

    public void initializeItems() {

        initBarrier();

        if (this.bills.isEmpty()) {
            CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(player);
            issuerTarget.sendInfo(MessageKeys.NO_AUCTION);
            return;
        }

        int id = (this.bidConfig.getItemBlocks().size() * this.page) - this.bidConfig.getItemBlocks().size();
        for (int index : bidConfig.getItemBlocks()) {
            inv.setItem(index, createGuiItem(bills.get(id)));
            id++;
            if (id >= (bills.size())) break;
        }
        openInventory(player);
    }

    private void initBarrier() {

        for (Barrier barrier : bidConfig.getBarrierBlocks()) {
            inv.setItem(barrier.getIndex(), createGuiItem(getItemStack(barrier, false)));
        }

        for (Barrier barrier : bidConfig.getGoToAuctionBlocks()) {
            inv.setItem(barrier.getIndex(), createGuiItem(getItemStack(barrier, false)));
        }

        for (Barrier barrier : bidConfig.getExpireBlocks()) {
            inv.setItem(barrier.getIndex(), createGuiItem(getItemStack(barrier, false)));
        }

        for (Barrier previous : bidConfig.getPreviousBlocks()) {
            if (page > 1) {
                inv.setItem(previous.getIndex(), createGuiItem(getItemStack(previous, false)));
            } else {
                inv.setItem(previous.getRemplacement().getIndex(), createGuiItem(getItemStack(previous, true)));
            }
        }

        for (Barrier next : bidConfig.getNextBlocks()) {
            if ((this.bidConfig.getItemBlocks().size() * this.page) - this.bidConfig.getItemBlocks().size() < bills.size() - this.bidConfig.getItemBlocks().size()) {
                inv.setItem(next.getIndex(), createGuiItem(getItemStack(next, false)));
            } else {
                inv.setItem(next.getRemplacement().getIndex(), createGuiItem(getItemStack(next, true)));
            }
        }

        for (Barrier player : bidConfig.getPlayerBlocks()) {
            inv.setItem(player.getIndex(), createGuiItem(getItemStack(player, false)));
        }

        for (Barrier close : bidConfig.getCloseBlocks()) {
            inv.setItem(close.getIndex(), createGuiItem(getItemStack(close, false)));
        }
    }

    private ItemStack createGuiItem(Bill bill) {
        SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yyyy 'a' HH:mm");
        ItemStack item = bill.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        String title = bidConfig.getTitle();
        if (item.getItemMeta().getDisplayName().equalsIgnoreCase("")) {
            title = title.replace("{ItemName}", item.getType().name().replace('_', ' ').toLowerCase());
        } else {
            title = title.replace("{ItemName}", item.getItemMeta().getDisplayName());
        }
        title = title.replace("{OwnerName}", bill.getPlayerName());
        title = title.replace("{Price}", String.valueOf(bill.getPrice()));
        if (bill.getPlayerBidderName() != null) {
            title = title.replace("{BidderName}", bill.getPlayerBidderName());
            title = title.replace("{Bet}", String.valueOf(bill.getBet()));
            title = title.replace("{BetDate}", formater.format(bill.getBetDate()));
        } else {
            title = title.replace("{BidderName}", "Personne");
            title = title.replace("{Bet}", String.valueOf(bill.getPrice()));
            title = title.replace("{BetDate}", "");
        }

        title = FormatUtil.format(title);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        List<String> listDescription = new ArrayList<>();

        for (String desc : bidConfig.getDescription()) {
            if (item.getItemMeta().getDisplayName().equalsIgnoreCase("")) {
                desc = desc.replace("{ItemName}", item.getType().name().replace('_', ' ').toLowerCase());
            } else {
                desc = desc.replace("{ItemName}", item.getItemMeta().getDisplayName());
            }

            desc = desc.replace("{TotalVente}", String.valueOf(this.bills.size()));
            desc = desc.replace("{OwnerName}", bill.getPlayerName());
            if (bill.getPlayerBidderName() != null) {
                desc = desc.replace("{BidderName}", bill.getPlayerBidderName());
                desc = desc.replace("{Bet}", String.valueOf(bill.getBet()));
                desc = desc.replace("{BetDate}", formater.format(bill.getBetDate()));
            } else {
                desc = desc.replace("{BidderName}", "");
                desc = desc.replace("{Bet}", "");
                desc = desc.replace("{BetDate}", "");
            }

            desc = desc.replace("{Price}", String.valueOf(bill.getPrice()));
            Date expireDate = new Date((bill.getDate().getTime() + globalConfig.getBidTime() * 1000L));
            desc = desc.replace("{ExpireTime}", formater.format(expireDate));
            if (desc.contains("lore")) {
                if (item.getLore() != null) {
                    listDescription.addAll(item.getLore());
                } else {
                    listDescription.add(desc.replace("{lore}", ""));
                }
            } else {
                desc = FormatUtil.format(desc);
                listDescription.add(desc);
            }
        }
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(listDescription);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createGuiItem(Material material, String name, List<String> description) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        name = FormatUtil.format(name);
        List<String> descriptions = new ArrayList<>();
        for (String desc : description) {

            desc = desc.replace("{TotalSale}", String.valueOf(this.bills.size()));
            desc = FormatUtil.format(desc);
            descriptions.add(desc);
        }
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(descriptions);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createGuiItem(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || meta.getDisplayName() == null || meta.getLore() == null) {
            return itemStack;
        }
        String name = FormatUtil.format(meta.getDisplayName());
        List<String> descriptions = new ArrayList<>();
        for (String desc : meta.getLore()) {

            desc = desc.replace("{TotalSale}", String.valueOf(this.bills.size()));
            desc = FormatUtil.format(desc);
            descriptions.add(desc);
        }
        meta.setDisplayName(name);
        meta.setLore(descriptions);
        itemStack.setItemMeta(meta);
        return itemStack;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (inv.getHolder() != this || e.getInventory() != inv || player != e.getWhoClicked()) {
            return;
        }
        e.setCancelled(true);

        if (globalConfig.isSecurityForSpammingPacket()) {
            LocalDateTime clickTest = LocalDateTime.now();
            boolean isSpamming = spamTest.stream().anyMatch(d -> d.getHour() == clickTest.getHour() && d.getMinute() == clickTest.getMinute() && (d.getSecond() == clickTest.getSecond() || d.getSecond() == clickTest.getSecond() + 1 || d.getSecond() == clickTest.getSecond() - 1));
            if (isSpamming) {
                plugin.getLogger().warning("Warning : Spam gui auction Pseudo : " + player.getName());
                CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(player);
                issuerTarget.sendInfo(MessageKeys.SPAM);
                return;
            } else {
                spamTest.add(clickTest);
            }
        }

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        for (Barrier previous : bidConfig.getPreviousBlocks()) {
            if (e.getRawSlot() == previous.getIndex() && this.page > 1) {
                TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                chain.asyncFirst(billCommandManager::getBills).syncLast(bills -> {
                    BidGui gui = new BidGui(plugin, player, bills, this.page - 1);
                    gui.initializeItems();
                }).execute();
            }
        }

        for (Barrier next : bidConfig.getNextBlocks()) {
            if (e.getRawSlot() == next.getIndex() && ((this.bidConfig.getItemBlocks().size() * this.page) - this.bidConfig.getItemBlocks().size() < bills.size() - this.bidConfig.getItemBlocks().size()) && next.getMaterial() != next.getRemplacement().getMaterial()) {
                TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                chain.asyncFirst(billCommandManager::getBills).syncLast(bills -> {
                    BidGui gui = new BidGui(plugin, player, bills, this.page + 1);
                    gui.initializeItems();
                }).execute();
                return;
            }
        }

        for (Barrier expire : bidConfig.getExpireBlocks()) {
            if (e.getRawSlot() == expire.getIndex()) {
                TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                chain.asyncFirst(() -> expireCommandManager.getExpires(player.getUniqueId())).syncLast(auctions -> {
                    ExpireGui gui = new ExpireGui(plugin, player, auctions, 1);
                    gui.initializeItems();
                }).execute();

                return;
            }
        }

        for (Barrier close : bidConfig.getCloseBlocks()) {
            if (e.getRawSlot() == close.getIndex()) {
                player.closeInventory();
                return;
            }
        }

        for (Barrier goToAuction : bidConfig.getGoToAuctionBlocks()) {
            if (e.getRawSlot() == goToAuction.getIndex()) {
                TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                chain.asyncFirst(auctionCommandManager::getAuctions).syncLast(auctions -> {
                    AuctionsGui gui = new AuctionsGui(plugin, player, auctions, 1);
                    gui.initializeItems();
                }).execute();
                return;
            }
        }

        for (Barrier expire : bidConfig.getPlayerBlocks()) {
            if (e.getRawSlot() == expire.getIndex()) {

                TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                chain.asyncFirst(billCommandManager::getBills).syncLast(bills -> {
                    PlayerViewBidGui gui = new PlayerViewBidGui(plugin, player, bills, 1);
                    gui.initializeItems();
                }).execute();

                return;
            }
        }

        for (int index : bidConfig.getItemBlocks()) {
            if (index == e.getRawSlot()) {
                int nb0 = bidConfig.getItemBlocks().get(0);
                int nb = (e.getRawSlot() - nb0) / 9;
                Bill bill = bills.get((e.getRawSlot() - nb0) + ((this.bidConfig.getItemBlocks().size() * this.page) - this.bidConfig.getItemBlocks().size()) - nb * 2);

                if (e.isRightClick()) {
                    TaskChain<Bill> chainBill = FAuction.newChain();
                    chainBill.asyncFirst(() -> billCommandManager.billExist(bill.getId())).syncLast(billA -> {
                        if (billA == null) {
                            return;
                        }

                        if (!billA.getPlayerUuid().equals(player.getUniqueId())) {
                            return;
                        }

                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItem(player.getLocation(), billA.getItemStack());
                        } else {
                            player.getInventory().addItem(bill.getItemStack());
                        }

                        billCommandManager.deleteBill(bill.getId());
                        bills.remove(bill);
                        CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(player);
                        issuerTarget.sendInfo(MessageKeys.REMOVE_BILL_SUCCESS);
                        inv.close();

                        TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
                        chain.asyncFirst(billCommandManager::getBills).syncLast(bills -> {
                            BidGui gui = new BidGui(plugin, player, bills, 1);
                            gui.initializeItems();
                        }).execute();

                    }).execute();
                } else if (e.isLeftClick()) {
                    CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(player);
                    if (bill.getPlayerUuid().equals(player.getUniqueId())) {
                        issuerTarget.sendInfo(MessageKeys.BUY_YOUR_ITEM);
                        return;
                    }
                    if (!plugin.getVaultIntegrationManager().getEconomy().has(player, bill.getPrice())) {
                        issuerTarget.sendInfo(MessageKeys.NO_HAVE_MONEY);
                        return;
                    }

                    inv.close();
                    AmountGui amountGui = new AmountGui(plugin, player, page, bill);
                    amountGui.initializeItems();
                }
            }
            break;
        }
    }
}
