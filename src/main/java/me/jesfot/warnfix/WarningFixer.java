package me.jesfot.warnfix;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WarningFixer extends JavaPlugin
{
	private final AtomicBoolean removed = new AtomicBoolean(false);
	
	public WarningFixer()
	{
		tryRemove();
	}
	
	@Override
	public void onLoad()
	{
		tryRemove();
	}
	
	@Override
	public void onEnable()
	{
		tryRemove();
	}
	
	@Override
	public void onDisable()
	{
		this.removed.set(false);
	}
	
	private void tryRemove()
	{
		if (!this.removed.get())
		{
			PluginManager manager = Bukkit.getPluginManager();
			if (manager != null && (manager instanceof SimplePluginManager))
			{
				SimplePluginManager simpleManager = (SimplePluginManager) manager;
				try
				{
					Field graphField = SimplePluginManager.class.getDeclaredField("dependencyGraph");
					if (graphField == null)
					{
						throw new Exception("Class#getDeclaredField returned null value");
					}
					graphField.setAccessible(true);
					Object depGraphValue = graphField.get(simpleManager);
					if (depGraphValue == null || !(depGraphValue instanceof MutableGraph))
					{
						throw new Exception("'dependencyGraph' value in SimplePluginManager is not a MutableGraph or is null");
					}
					@SuppressWarnings("unchecked")
					MutableGraph<String> dependencyGraph = (MutableGraph<String>) depGraphValue;
					if (!dependencyGraph.isDirected())
					{
						return;
					}
					MutableGraph<String> replacement = GraphBuilder.undirected().build();
					for (String node : dependencyGraph.nodes())
					{
						if (node == null || node.isEmpty())
						{
							continue;
						}
						for (String linked : Graphs.reachableNodes(dependencyGraph, node))
						{
							if (linked != null && !linked.isEmpty() && !linked.equalsIgnoreCase(node))
							{
								replacement.putEdge(node, linked);
							}
						}
					}
					
					graphField.set(simpleManager, replacement);
					this.removed.set(true);
					Logger logger = getLogger();
					if (logger != null)
					{
						logger.log(Level.INFO, "Warnings about loaded classes not being parts of dependent plugins have been disabled !");
						logger.log(Level.INFO, "NB: LuckPerms 'javassist.d' class warning cannot be removed with the same method.");
					}
				}
				catch (Exception ex)
				{
					Logger logger = getLogger();
					if (logger != null)
					{
						logger.log(Level.INFO, "Could not fix dependencies warnings", ex);
					}
				}
			}
			else
			{
				Logger logger = getLogger();
				if (logger != null)
				{
					logger.log(Level.INFO, "No plugin manager to hook into (or is not a SimplePluginManager)");
				}
			}
		}
	}
}
