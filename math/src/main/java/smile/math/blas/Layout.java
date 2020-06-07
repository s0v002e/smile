/*******************************************************************************
 * Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package smile.math.blas;

/** Matrix layout. */
public enum Layout {
    /** Row major layout. */
    RowMajor(101),
    /** Column major layout. */
    ColMajor(102);

    /** Integer value passed to CBLAS. */
    private final int value;

    /** Constructor. */
    Layout(int value) {
        this.value = value;
    }

    /** Returns the integer value for BLAS. */
    public int getValue() { return value; }
}