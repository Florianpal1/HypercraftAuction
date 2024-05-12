package fr.florianpal.fauction.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import co.aikar.taskchain.TaskChain;
import fr.florianpal.fauction.FAuction;
import fr.florianpal.fauction.configurations.GlobalConfig;
import fr.florianpal.fauction.gui.subGui.AuctionsGui;
import fr.florianpal.fauction.gui.subGui.ExpireGui;
import fr.florianpal.fauction.languages.MessageKeys;
import fr.florianpal.fauction.managers.commandManagers.AuctionCommandManager;
import fr.florianpal.fauction.managers.commandManagers.CommandManager;
import fr.florianpal.fauction.managers.commandManagers.ExpireCommandManager;
import fr.florianpal.fauction.objects.Auction;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.ceil;

@CommandAlias("ah|hdv")
public class AuctionCommand extends BaseCommand {

    private final CommandManager commandManager;
    private final AuctionCommandManager auctionCommandManager;

    private final ExpireCommandManager expireCommandManager;

    private final FAuction plugin;

    private final GlobalConfig globalConfig;

    private final List<LocalDateTime> spamTest = new ArrayList<>();

    public AuctionCommand(FAuction plugin) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.auctionCommandManager = plugin.getAuctionCommandManager();
        this.expireCommandManager = plugin.getExpireCommandManager();
        this.globalConfig = plugin.getConfigurationManager().getGlobalConfig();
    }

    @Default
    @Subcommand("list")
    @CommandPermission("fauction.list")
    @Description("{@@fauction.auction_list_help_description}")
    public void onList(Player playerSender) {
        if (globalConfig.isSecurityForSpammingPacket()) {
            LocalDateTime clickTest = LocalDateTime.now();
            boolean isSpamming = spamTest.stream().anyMatch(d -> d.getHour() == clickTest.getHour() && d.getMinute() == clickTest.getMinute() && (d.getSecond() == clickTest.getSecond() || d.getSecond() == clickTest.getSecond() + 1 || d.getSecond() == clickTest.getSecond() - 1));
            if (isSpamming) {
                plugin.getLogger().warning("Warning : Spam command list. Pseudo : " + playerSender.getName());
                CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(playerSender);
                issuerTarget.sendInfo(MessageKeys.SPAM);
                return;
            } else {
                spamTest.add(clickTest);
            }
        }


        TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
        chain.asyncFirst(auctionCommandManager::getAuctions).sync(auctions -> {
            AuctionsGui gui = new AuctionsGui(plugin, playerSender, auctions, 1);
            gui.initializeItems();
            CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
            issuerTarget.sendInfo(MessageKeys.AUCTION_OPEN);
            return null;
        }).execute();
    }

    @Subcommand("sell")
    @CommandPermission("fauction.sell")
    @Description("{@@fauction.auction_add_help_description}")
    public void onAdd(Player playerSender, double price) {

        if (globalConfig.isSecurityForSpammingPacket()) {
            LocalDateTime clickTest = LocalDateTime.now();
            boolean isSpamming = spamTest.stream().anyMatch(d -> d.getHour() == clickTest.getHour() && d.getMinute() == clickTest.getMinute() && (d.getSecond() == clickTest.getSecond() || d.getSecond() == clickTest.getSecond() + 1 || d.getSecond() == clickTest.getSecond() - 1));
            if (isSpamming) {
                plugin.getLogger().warning("Warning : Spam command sell Pseudo : " + playerSender.getName());
                CommandIssuer issuerTarget = plugin.getCommandManager().getCommandIssuer(playerSender);
                issuerTarget.sendInfo(MessageKeys.SPAM);
                return;
            } else {
                spamTest.add(clickTest);
            }
        }

        CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
        TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
        chain.asyncFirst(() -> plugin.getAuctionCommandManager().getAuctions(playerSender.getUniqueId())).sync(auctions -> {
            if (plugin.getLimitationManager().getAuctionLimitation(playerSender) <= auctions.size()) {
                issuerTarget.sendInfo(MessageKeys.MAX_AUCTION);
                return null;
            }
            if (price < 0) {
                issuerTarget.sendInfo(MessageKeys.NEGATIVE_PRICE);
                return null;
            }
            if (playerSender.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                issuerTarget.sendInfo(MessageKeys.ITEM_AIR);
                return null;
            }

            if(globalConfig.getMinPrice().containsKey(playerSender.getInventory().getItemInMainHand().getType())) {
                double minPrice = playerSender.getInventory().getItemInMainHand().getAmount() *  globalConfig.getMinPrice().get(playerSender.getInventory().getItemInMainHand().getType());
                if(minPrice > price) {
                    issuerTarget.sendInfo(MessageKeys.MIN_PRICE, "{minPrice}", String.valueOf(ceil(minPrice)));
                    return null;
                }
            } else if (globalConfig.isDefaultMinValueEnable()) {
                double minPrice = playerSender.getInventory().getItemInMainHand().getAmount() *  globalConfig.getDefaultMinValue();
                if(minPrice > price) {
                    issuerTarget.sendInfo(MessageKeys.MIN_PRICE, "{minPrice}", String.valueOf(ceil(minPrice)));
                    return null;
                }
            }

            if(globalConfig.getMaxPrice().containsKey(playerSender.getInventory().getItemInMainHand().getType())) {
                double maxPrice = playerSender.getInventory().getItemInMainHand().getAmount() *  globalConfig.getMaxPrice().get(playerSender.getInventory().getItemInMainHand().getType());
                if(maxPrice < price) {
                    issuerTarget.sendInfo(MessageKeys.MAX_PRICE, "{maxPrice}", String.valueOf(ceil(maxPrice)));
                    return null;
                }
            } else if (globalConfig.isDefaultMaxValueEnable()) {
                double maxPrice = playerSender.getInventory().getItemInMainHand().getAmount() *  globalConfig.getDefaultMaxValue();
                if(maxPrice < price) {
                    issuerTarget.sendInfo(MessageKeys.MAX_PRICE, "{maxPrice}", String.valueOf(ceil(maxPrice)));
                    return null;
                }
            }

            if(Tag.SHULKER_BOXES.getValues().contains(playerSender.getInventory().getItemInMainHand().getType())) {
                ItemStack item = playerSender.getInventory().getItemInMainHand();
                if (item.getItemMeta() instanceof BlockStateMeta) {
                    double minPrice = 0;
                    double maxPrice = 0;
                    BlockStateMeta im = (BlockStateMeta) item.getItemMeta();
                    if (im.getBlockState() instanceof ShulkerBox) {
                        ShulkerBox shulker = (ShulkerBox) im.getBlockState();
                        for (ItemStack itemIn : shulker.getInventory().getContents()) {
                            if (itemIn != null && (itemIn.getType() != Material.AIR)) {
                                if (plugin.getConfigurationManager().getGlobalConfig().getMinPrice().containsKey(itemIn.getType())) {
                                    minPrice = minPrice + itemIn.getAmount() * globalConfig.getMinPrice().get(itemIn.getType());
                                } else if (plugin.getConfigurationManager().getGlobalConfig().isDefaultMinValueEnable()) {
                                    minPrice = minPrice + itemIn.getAmount() * globalConfig.getDefaultMinValue();
                                }

                                if (plugin.getConfigurationManager().getGlobalConfig().getMaxPrice().containsKey(itemIn.getType())) {
                                    maxPrice = maxPrice + itemIn.getAmount() * globalConfig.getMaxPrice().get(itemIn.getType());
                                } else if (plugin.getConfigurationManager().getGlobalConfig().isDefaultMaxValueEnable()) {
                                    maxPrice = maxPrice + itemIn.getAmount() * globalConfig.getDefaultMaxValue();
                                }
                            }
                        }
                        if (minPrice > price) {
                            issuerTarget.sendInfo(MessageKeys.MIN_PRICE, "{minPrice}", String.valueOf(ceil(minPrice)));
                            return null;
                        }

                        if (maxPrice < price) {
                            issuerTarget.sendInfo(MessageKeys.MAX_PRICE, "{maxPrice}", String.valueOf(ceil(maxPrice)));
                            return null;
                        }
                    }
                }
            }

            String itemName = playerSender.getInventory().getItemInMainHand().getItemMeta().getDisplayName() == null || playerSender.getInventory().getItemInMainHand().getItemMeta().getDisplayName().isEmpty() ? playerSender.getInventory().getItemInMainHand().getType().toString() : playerSender.getInventory().getItemInMainHand().getItemMeta().getDisplayName();
            plugin.getLogger().info("Player " + playerSender.getName() + " add item to ah Item : " + itemName + ", At Price : " + price);
            auctionCommandManager.addAuction(playerSender, playerSender.getInventory().getItemInMainHand(), price);
            playerSender.getInventory().getItemInMainHand().setAmount(0);
            issuerTarget.sendInfo(MessageKeys.AUCTION_ADD_SUCCESS);
            return null;
        }).execute();
    }

    @Subcommand("expire")
    @CommandPermission("fauction.expire")
    @Description("{@@fauction.expire_add_help_description}")
    public void onExpire(Player playerSender) {

        TaskChain<ArrayList<Auction>> chain = FAuction.newChain();
        chain.asyncFirst(() -> expireCommandManager.getAuctions(playerSender.getUniqueId())).sync(auctions -> {
            ExpireGui gui = new ExpireGui(plugin, playerSender, auctions, 1);
            gui.initializeItems();
            CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
            issuerTarget.sendInfo(MessageKeys.AUCTION_OPEN);
            return null;
        }).execute();

    }

    @Subcommand("admin reload")
    @CommandPermission("fauction.admin.reload")
    @Description("{@@fauction.reload_help_description}")
    public void onReload(Player playerSender) {
        CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
        plugin.reloadConfig();
        issuerTarget.sendInfo(MessageKeys.AUCTION_RELOAD);
    }

    @Subcommand("admin transfertToPaper")
    @CommandPermission("fauction.admin.transfertBddToPaper")
    @Description("{@@fauction.transfert_bdd_help_description}")
    public void onTransferBddPaper(Player playerSender) {
        CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
        plugin.transfertBDD(true);
        issuerTarget.sendInfo(MessageKeys.TRANSFERT_BDD);
    }

    @Subcommand("admin transfertToBukkit")
    @CommandPermission("fauction.admin.transfertBddToPaper")
    @Description("{@@fauction.transfert_bdd_help_description}")
    public void onTransferBddSpigot(Player playerSender) {
        CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
        plugin.transfertBDD(false);
        issuerTarget.sendInfo(MessageKeys.TRANSFERT_BDD);
    }

    @Subcommand("admin clearCache")
    @CommandPermission("fauction.admin.clearCache")
    @Description("{@@fauction.clear_cache_help_description}")
    public void onClearCache(Player playerSender) {
        CommandIssuer issuerTarget = commandManager.getCommandIssuer(playerSender);
        plugin.clearCache();
        issuerTarget.sendInfo(MessageKeys.CLEAR_CACHE);
    }

    @HelpCommand
    @Description("{@@fauction.help_description}")
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }
}