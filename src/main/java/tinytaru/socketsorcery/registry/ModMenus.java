package tinytaru.socketsorcery.registry;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import tinytaru.socketsorcery.SocketSorcery;
import tinytaru.socketsorcery.menu.EngravingTableMenu;
import tinytaru.socketsorcery.menu.SocketingBenchMenu;

public final class ModMenus {

	public static final MenuType<EngravingTableMenu> ENGRAVING_TABLE = register("engraving_table",
			new ExtendedMenuType<>(EngravingTableMenu::new, BlockPos.STREAM_CODEC));

	public static final MenuType<SocketingBenchMenu> SOCKETING_BENCH = register("socketing_bench",
			new ExtendedMenuType<>(SocketingBenchMenu::new, BlockPos.STREAM_CODEC));

	private static <T extends AbstractContainerMenu> MenuType<T> register(String name, MenuType<T> type) {
		return Registry.register(BuiltInRegistries.MENU, SocketSorcery.id(name), type);
	}

	public static void init() {
	}

	private ModMenus() {
	}
}
