/*
 * Copyright (C) 2012-2015 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.money.manager.ex.database;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.money.manager.ex.core.AccountTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Accounts
 */
public class AccountRepository {
    public AccountRepository(Context context) {
        mContext = context;
        mAccount = new TableAccountList();
    }

    private Context mContext;
    private TableAccountList mAccount;

    public int loadIdByName(String name) {
        int result = -1;

        if(TextUtils.isEmpty(name)) { return result; }

        String selection = TableAccountList.ACCOUNTNAME + "=?";

        Cursor cursor = mContext.getContentResolver().query(
                mAccount.getUri(),
                new String[] { TableAccountList.ACCOUNTID },
                selection,
                new String[] { name },
                null);

        if(cursor.moveToFirst()) {
            result = cursor.getInt(cursor.getColumnIndex(TableAccountList.ACCOUNTID));
        }

        cursor.close();

        return result;
    }

    /**
     * @param open     show open accounts
     * @param favorite show favorite account
     * @return List<TableAccountList> list of accounts selected
     */
    public List<TableAccountList> getListAccounts(boolean open, boolean favorite) {
        // create a return list
        List<TableAccountList> listAccount = loadAccounts(open, favorite, null);
        return listAccount;
    }

    public List<TableAccountList> getTransactionAccounts(boolean open, boolean favorite) {
        ArrayList<String> accountTypes = new ArrayList<>();
        accountTypes.add(AccountTypes.CHECKING.toString());
        accountTypes.add(AccountTypes.TERM.toString());
        accountTypes.add(AccountTypes.CREDIT_CARD.toString());

        List<TableAccountList> result = loadAccounts(open, favorite, accountTypes);

        return result;
    }

    public List<TableAccountList> loadAccounts(boolean open, boolean favorite, List<String> accountTypes) {
        // create a return list
        List<TableAccountList> listAccount = new ArrayList<>();

        // compose where clause
        String where = getWhereFilterFor(open,favorite);

        // filter accounts.
        if (accountTypes != null && accountTypes.size() > 0) {
            where = DatabaseUtils.concatenateWhere(where, getWherePartFor(accountTypes));
        }

        // data cursor
        TableAccountList tAccountList = new TableAccountList();
        MoneyManagerOpenHelper helper = MoneyManagerOpenHelper.getInstance(mContext.getApplicationContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        if (db != null) {
            Cursor cursor = db.query(tAccountList.getSource(), tAccountList.getAllColumns(),
                    where, null, null, null, TableAccountList.ACCOUNTNAME);
            // populate list from data cursor
            if (cursor != null && cursor.moveToFirst()) {
                while (!(cursor.isAfterLast())) {
                    TableAccountList account = new TableAccountList();
                    account.setValueFromCursor(cursor);
                    listAccount.add(account);
                    cursor.moveToNext();
                }
                cursor.close();
            }
//            db.close();
        }
        return listAccount;
    }

    private String getWhereFilterFor(boolean open, boolean favorite) {
        StringBuilder where = new StringBuilder();

        if (open) {
            where.append("LOWER(STATUS)='open'");
        }
        if (favorite) {
            if (open) {
                where.append(" AND ");
            }
            where.append("LOWER(FAVORITEACCT)='true'");
        }

        return where.toString();
    }

    private String getWherePartFor(List<String> accountTypes) {
        StringBuilder where = new StringBuilder();
        where.append(mAccount.ACCOUNTTYPE);
        where.append(" IN (");
        for(String type : accountTypes) {
            if (accountTypes.indexOf(type) > 0) {
                where.append(',');
            }

            where.append("'");
            where.append(type);
            where.append("'");
        }
        where.append(")");

        return where.toString();
    }
}
