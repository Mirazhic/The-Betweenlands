package thebetweenlands.api.environment;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public interface IEnvironmentEventRegistry {
	/**
	 * Registers an environment event
	 * @param event
	 */
	public void register(EnvironmentEvent event);

	/**
	 * Unregisters an environment event
	 * @param event
	 * @return
	 */
	public EnvironmentEvent unregister(EnvironmentEvent event);

	/**
	 * Returns the world this environment event registry belongs to
	 * @return
	 */
	public World getWorld();

	/**
	 * Returns an unmodifiable map of all registered environment events
	 * @return
	 */
	public Map<ResourceLocation, EnvironmentEvent> getEvents();

	/**
	 * Returns the event with the specified ID or null if that event is not registered
	 * @param eventId
	 * @return
	 */
	@Nullable
	public EnvironmentEvent getEvent(ResourceLocation eventId);

	/**
	 * Returns whether the specified event is registered and {@link EnvironmentEvent#isActive()} == true
	 * @param eventId
	 * @return
	 */
	public boolean isEventActive(ResourceLocation eventId);

	/**
	 * Returns whether the specified event is registered and {@link EnvironmentEvent#isActiveAt(double, double, double)} == true
	 * @param x
	 * @param y
	 * @param z
	 * @param eventId
	 * @return
	 */
	public boolean isEventActiveAt(double x, double y, double z, ResourceLocation eventId);

	/**
	 * Returns a list of all registered events whose {@link EnvironmentEvent#isActive()} == active
	 * @param active
	 * @return
	 */
	public List<EnvironmentEvent> getEventsOfState(boolean active);

	/**
	 * Returns a list of all registered events whose {@link EnvironmentEvent#isActiveAt(double, double, double)} == active
	 * @param x
	 * @param y
	 * @param z
	 * @param active
	 * @return
	 */
	public List<EnvironmentEvent> getEventsOfStateAt(double x, double y, double z, boolean active);

	/**
	 * Sets whether the registry is enabled. Environment events are only updated
	 * if the registry is enabled
	 * @param enabled
	 * @return
	 */
	public boolean setEnabled(boolean enabled);

	/**
	 * Returns whether the registry is enabled
	 * @return
	 */
	public boolean isEnabled();
}
