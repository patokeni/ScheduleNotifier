package xianxian.center.schedulenotifier;

import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemLongClick;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.main.IFragment;
import xianxian.center.main.Main;

/**
 * Created by xiaoyixian on 18-6-5.
 */

public class TypesFragment extends Fragment implements IFragment {


    @BindView(R2.id.listViewTypes)
    ListView listViewTypes;
    Unbinder unbinder;
    Adapters.TypesAdapter typesAdapter;
    private Callback onContextMenuSelected;
    private int contextMenuRes;
    private String contextMenuTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_sn_types, container, false);
        unbinder = ButterKnife.bind(this, view);
        typesAdapter = new Adapters.TypesAdapter(getContext());
        registerForContextMenu(listViewTypes);
        listViewTypes.setAdapter(typesAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnItemLongClick(R2.id.listViewTypes)
    public boolean onItemLongClicked(int pos) {
        Type type = typesAdapter.getItem(pos);

        showContextMenu(listViewTypes, "类型操作", R.menu.menu_sn_type_operation, new Callback() {
            @Override
            public boolean Do(Object... objects) {
                MenuItem menuItem = (MenuItem) objects[0];
                if (menuItem.getItemId() == R.id.menu_sn_type_oper_set_custom_message) {
                    EditText editText = new EditText(getContext());
                    editText.setHint("输入自定义提醒");
                    new AlertDialog.Builder(getContext())
                            .setTitle("设置自定义提醒")
                            .setView(editText)
                            .setPositiveButton("确定", (dialog, which) -> {
                                type.setCustomMessage(editText.getText().toString());
                                dialog.cancel();
                            })
                            .setNegativeButton("取消", (dialog, whick) -> {
                                dialog.cancel();
                            })
                            .show();
                } else if (menuItem.getItemId() == R.id.menu_sn_type_oper_delete) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("确定？")
                            .setMessage("你确定要删除此类型，" +
                                    "注意，只有当没有计划项目使用此类型时才会彻底删除，" +
                                    "否则只将提醒设置为默认")
                            .setPositiveButton("删除", (dialog, which) -> {
                                boolean isCustomMessage = type.isCustomMessage();
                                String customMessage = type.getMessage();
                                Main.getMainActivity().prepareSnackbar(type.hasUser() ? "已删除" : "已将提醒设置为默认", Snackbar.LENGTH_LONG)
                                        .setAction("撤销", (view) -> {
                                            if (isCustomMessage) {
                                                type.setCustomMessage(customMessage);
                                            }
                                            Types.addType(type);
                                        })
                                        .show();
                                Types.removeType(type);
                            })
                            .setNegativeButton("取消", ((dialog, which) -> {
                                dialog.cancel();
                            }))
                            .show();
                }
                return true;
            }
        });
        return true;
    }

    private void showContextMenu(View view, String title, @MenuRes int menuRes, Callback onContextMenuSelected) {
        this.onContextMenuSelected = onContextMenuSelected;
        this.contextMenuTitle = title;
        this.contextMenuRes = menuRes;
        view.showContextMenu();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (onContextMenuSelected != null)
            onContextMenuSelected.Do(item);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(contextMenuRes, menu);
        menu.setHeaderTitle(contextMenuTitle);
    }

}
