package com.commodore.verifyme.command;

import com.commodore.verifyme.VerifyMe;
import com.commodore.verifyme.util.LinkedAccountType;
import java.util.Date;
import me.totalfreedom.totalfreedommod.admin.Admin;
import me.totalfreedom.totalfreedommod.rank.Displayable;
import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import net.pravian.aero.util.Ips;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Command_forum implements CommandExecutor
{
    private VerifyMe plugin;
    
    public Command_forum(VerifyMe plugin)
    {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        if(!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "This command can only be used ingame.");
            return true;
        }
        
        Player playerSender = (Player) sender;
        String name = playerSender.getName();
        switch(args.length)
        {
            case 0:
                if(!plugin.futils.enabled)
                {
                    playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                    return true;
                }
                
                playerSender.sendMessage(ChatColor.RED + "You didn't specify enough arguments.");
                return false;
            case 1:
                switch(args[0])
                {
                    case "linkaccount":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!plugin.tfm.al.isAdmin(sender))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not authorized to use this command!");
                            return true;
                        }
                        
                        Admin linkAdmin = plugin.tfm.al.getAdmin(playerSender);
                        if(plugin.sutils.hasAlreadyLinkedAccount(linkAdmin.getName(), LinkedAccountType.FORUM))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You have already linked a forum account to your account!");
                            return true;
                        }
                        if(plugin.futils.LINK_CODES.keySet().contains(linkAdmin))
                        {
                            playerSender.sendMessage(ChatColor.RED + "The specified forum account has already had a linking token sent to it.");
                            String token = plugin.futils.LINK_CODES.get(linkAdmin);
                            playerSender.sendMessage(ChatColor.AQUA + "Your linking token is " + ChatColor.GREEN + token);
                            return true;
                        }
                        
                        plugin.vlog.info(name + " has begun the forum account link process.");
                        String linkingToken = plugin.generateToken();
                        plugin.futils.LINK_CODES.put(linkAdmin, linkingToken);
                        playerSender.sendMessage(ChatColor.AQUA + "Your linking token is " + ChatColor.GREEN + linkingToken);
                        playerSender.sendMessage(ChatColor.AQUA + "Please PM the forum bot named " + plugin.futils.botName + " with your token otherwise it will expire in 10 minutes.");
                        plugin.futils.findNewPmTask(playerSender);
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                if(plugin.futils.LINK_CODES.keySet().contains(linkAdmin))
                                {
                                    plugin.vlog.info(name + "'s forum linking token has expired.");
                                    plugin.futils.LINK_CODES.remove(linkAdmin);
                                    if(playerSender != null)
                                    {
                                        playerSender.sendMessage(ChatColor.RED + "Your linking token has expired! Please run this command again to obtain a new one.");
                                    }
                                }
                            }
                        }.runTaskLater(plugin, 600 * 20L);
                        return true;
                    case "unlinkaccount":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!plugin.tfm.al.isAdmin(sender))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not authorized to use this command!");
                            return true;
                        }
                        
                        Admin unlinkAdmin = plugin.tfm.al.getAdmin(playerSender);
                        if(!plugin.sutils.hasAlreadyLinkedAccount(unlinkAdmin.getName(), LinkedAccountType.FORUM))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You have not got a forum account linked to this account!");
                            return true;
                        }
                        
                        plugin.vlog.info(name + " has unlinked their forum account.");
                        plugin.sutils.deleteAccountFromStorage(unlinkAdmin.getName(), LinkedAccountType.FORUM);
                        playerSender.sendMessage(ChatColor.GREEN + "Your forum account has been unlinked from this account.");
                        return true;
                    case "gettoken":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!plugin.tfm.al.isAdminImpostor(playerSender))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not an impostor!");
                            return true;
                        }
                        
                        Admin verifyAdmin = plugin.tfm.al.getEntryByName(playerSender.getName());
                        if(!plugin.sutils.hasAlreadyLinkedAccount(verifyAdmin.getName(), LinkedAccountType.FORUM))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You have not got a forum account linked to this account!");
                            return true;
                        }
                        if(plugin.futils.VERIFY_CODES.keySet().contains(verifyAdmin))
                        {
                            playerSender.sendMessage(ChatColor.RED + "The specified forum account has already had a verification token sent to it.");
                            return true;
                        }
                        
                        plugin.vlog.info(name + " has obtained a forum verification code.");
                        FUtil.bcastMsg(playerSender.getName() + " is verifying using Forum Verification!", ChatColor.GOLD);
                        String verifyToken = plugin.generateToken();
                        plugin.futils.VERIFY_CODES.put(verifyAdmin, verifyToken);
                        plugin.futils.sendNewPmTask(plugin.sutils.getForumUsername(verifyAdmin), "Verify your account.", "Hi! Someone with the IP: " + Ips.getIp(playerSender) + " just logged in with your account and tried to verify. If this is you please run the command: /forum verifytoken " + verifyToken);
                        playerSender.sendMessage(ChatColor.GREEN + "A verification token has been sent to your forum account. It will expire in 10 minutes.");
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                if(plugin.futils.VERIFY_CODES.keySet().contains(verifyAdmin))
                                {
                                    plugin.vlog.info(name + "'s forum verification token has expired.");
                                    plugin.futils.VERIFY_CODES.remove(verifyAdmin);
                                    if(playerSender != null)
                                    {
                                        playerSender.sendMessage(ChatColor.RED + "Your verification token has expired! Please run this command again to obtain a new one.");
                                    }
                                }
                            }
                        }.runTaskLater(plugin, 600 * 20L);
                        return true;
                    case "verifytoken":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        
                        playerSender.sendMessage(ChatColor.RED + "You specified an invalid amount of arguments.");
                        return false;
                    case "help":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!(plugin.tfm.al.isAdmin(playerSender) || plugin.tfm.al.isAdminImpostor(playerSender)))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not authorised to use this command!");
                            return true;
                        }
                        
                        playerSender.sendMessage(ChatColor.GREEN + "VerifyMe Forum Verification Usage");
                        
                        playerSender.sendMessage(ChatColor.RED + "As a supered admin:");
                        playerSender.sendMessage(ChatColor.BLUE + "1. Run the command /forum linkaccount");
                        playerSender.sendMessage(ChatColor.BLUE + "2. Copy the code that command gave you and jump on the forums, from there PM the bot named " + plugin.futils.botName + " with the token.");
                        playerSender.sendMessage(ChatColor.BLUE + "3. Make sure the body of the message contains the token, the subject can be anything.");
                        playerSender.sendMessage(ChatColor.BLUE + "4. After a couple seconds you should get a confirmation message in chat. Your account is linked!");
                        
                        playerSender.sendMessage(ChatColor.RED + "As an impostor:");
                        playerSender.sendMessage(ChatColor.BLUE + "1. Run the command /forum gettoken");
                        playerSender.sendMessage(ChatColor.BLUE + "2. Jump on the forums and you should find a PM containing the IP of the impostor and a token has been sent to you.");
                        playerSender.sendMessage(ChatColor.BLUE + "3. Copy the command and run it ingame.");
                        playerSender.sendMessage(ChatColor.BLUE + "4. You are now supered!");
                        return true;
                    default:
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        
                        playerSender.sendMessage(ChatColor.RED + "You specified an invalid argument.");
                        return false;
                }
            case 2:
                switch(args[0])
                {
                    case "verifytoken":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!plugin.tfm.al.isAdminImpostor(playerSender))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not an impostor!");
                            return true;
                        }
                        
                        Admin admin = plugin.tfm.al.getEntryByName(playerSender.getName());
                        if(!plugin.futils.VERIFY_CODES.keySet().contains(admin))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You have not been given a token.");
                            return true;
                        }
                        
                        String token = args[1];
                        if(!plugin.futils.VERIFY_CODES.get(admin).equals(token))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You have entered an invalid token. Please try again.");
                            return true;
                        }
                        
                        plugin.vlog.info(name + " has verified their account using the forums.");
                        plugin.futils.VERIFY_CODES.remove(admin);
                        FUtil.bcastMsg(playerSender.getName() + " has verified their identity.", ChatColor.GOLD);
                        FUtil.adminAction("VerifyMe", "Re-adding " + admin.getName() + " to the admin list", true);
                        admin.setName(playerSender.getName());
                        admin.addIp(Ips.getIp(playerSender));
                        admin.setActive(true);
                        admin.setLastLogin(new Date());
                        plugin.tfm.al.save();
                        plugin.tfm.al.updateTables();
                        final Displayable display = plugin.tfm.rm.getDisplay(playerSender);
                        plugin.tfm.pl.getPlayer(playerSender).setTag(display.getColoredTag());
                        String displayName = display.getColor() + playerSender.getName();
                        playerSender.setPlayerListName(StringUtils.substring(displayName, 0, 16));
                        return true;
                    case "unlinkaccount":
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        if(!(plugin.tfm.al.isAdmin(playerSender) && plugin.tfm.rm.getRank(playerSender) == Rank.SENIOR_ADMIN))
                        {
                            playerSender.sendMessage(ChatColor.RED + "You are not authorized to use this command!");
                            return true;
                        }
                        
                        String adminName = args[1];
                        if(!plugin.sutils.hasAlreadyLinkedAccount(adminName, LinkedAccountType.FORUM))
                        {
                            playerSender.sendMessage(ChatColor.RED + adminName + " does not have a forum account linked to this account!");
                            return true;
                        }
                        
                        plugin.vlog.info(name + " has unlinked " + adminName + "'s forum account.");
                        plugin.sutils.deleteAccountFromStorage(adminName, LinkedAccountType.FORUM);
                        playerSender.sendMessage(ChatColor.GREEN + adminName + " has had their forum account unlinked from this account.");
                        return true;
                    default:
                        if(!plugin.futils.enabled)
                        {
                            playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                            return true;
                        }
                        
                        playerSender.sendMessage(ChatColor.RED + "You specified an invalid argument.");
                        return false;
                }
            default:
                if(!plugin.futils.enabled)
                {
                    playerSender.sendMessage(ChatColor.RED + "The VerifyMe Forum Verification System is currently disabled.");
                    return true;
                }
                
                playerSender.sendMessage(ChatColor.RED + "You specified an invalid amount of arguments.");
                return false;
        }
    }
}