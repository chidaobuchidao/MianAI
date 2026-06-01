-- =============================================
-- V13__seed_interview_bank_expansion.sql
-- 扩充笔试算法题与八股文题库
-- =============================================

CREATE TABLE IF NOT EXISTS algorithm_problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    starter_code JSON NOT NULL,
    test_cases JSON NOT NULL,
    solution_code JSON NOT NULL,
    category VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_algorithm_problem_difficulty (difficulty),
    INDEX idx_algorithm_problem_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔试算法题库';

-- 确保分类存在，避免老库缺少分类时导入失败
INSERT INTO question_category (name, icon, sort_order)
SELECT '计算机网络', 'network', 1
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '计算机网络');

INSERT INTO question_category (name, icon, sort_order)
SELECT '操作系统', 'os', 2
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '操作系统');

INSERT INTO question_category (name, icon, sort_order)
SELECT '数据结构', 'data-structure', 3
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '数据结构');

INSERT INTO question_category (name, icon, sort_order)
SELECT '算法', 'algorithm', 4
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '算法');

INSERT INTO question_category (name, icon, sort_order)
SELECT '数据库', 'database', 5
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '数据库');

INSERT INTO question_category (name, icon, sort_order)
SELECT 'Java', 'java', 6
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = 'Java');

INSERT INTO question_category (name, icon, sort_order)
SELECT '设计模式', 'design-pattern', 7
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '设计模式');

INSERT INTO question_category (name, icon, sort_order)
SELECT '计算机组成原理', 'computer-org', 8
WHERE NOT EXISTS (SELECT 1 FROM question_category WHERE name = '计算机组成原理');

-- ==================== 笔试算法题：本地化 stdin/stdout 版本 ====================
INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '两数之和',
    '输入第一行是整数 n，第二行是 n 个整数，第三行是目标值 target。请输出两个下标，使得对应元素之和等于 target。保证恰有一组答案，下标从 0 开始，按升序输出，用空格分隔。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\nimport java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        String[] parts = br.readLine().trim().split("\\\\s+");\n        int target = Integer.parseInt(br.readLine().trim());\n        // TODO: 输出两个下标，例如: 0 1\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    target = int(input().strip())\n    # TODO: 输出两个下标，例如: 0 1\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst n = Number(lines[0]);\nconst nums = lines[1].trim().split(/\\s+/).map(Number);\nconst target = Number(lines[2]);\n// TODO: 输出两个下标，例如: 0 1',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    int target; cin >> target;\n    // TODO: 输出两个下标\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    var target int; fmt.Scan(&target)\n    // TODO: 输出两个下标\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '4\n2 7 11 15\n9', 'expected', '0 1'),
        JSON_OBJECT('input', '3\n3 2 4\n6', 'expected', '1 2'),
        JSON_OBJECT('input', '2\n3 3\n6', 'expected', '0 1')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; import java.util.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); String[] p = br.readLine().trim().split("\\\\s+"); int target = Integer.parseInt(br.readLine().trim()); Map<Integer,Integer> seen = new HashMap<>(); for (int i = 0; i < n; i++) { int x = Integer.parseInt(p[i]); if (seen.containsKey(target - x)) { System.out.println(seen.get(target - x) + " " + i); return; } seen.put(x, i); } } }',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    target = int(input().strip())\n    seen = {}\n    for i, x in enumerate(nums):\n        if target - x in seen:\n            print(seen[target - x], i)\n            return\n        seen[x] = i\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const n = Number(lines[0]); const nums = lines[1].trim().split(/\\s+/).map(Number); const target = Number(lines[2]); const seen = new Map(); for (let i = 0; i < n; i++) { const need = target - nums[i]; if (seen.has(need)) { console.log(`${seen.get(need)} ${i}`); process.exit(0); } seen.set(nums[i], i); }',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    int target; cin >> target;\n    unordered_map<int,int> seen;\n    for (int i = 0; i < n; i++) {\n        auto it = seen.find(target - nums[i]);\n        if (it != seen.end()) { cout << it->second << " " << i; return 0; }\n        seen[nums[i]] = i;\n    }\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    var target int; fmt.Scan(&target)\n    seen := make(map[int]int)\n    for i, x := range nums {\n        if j, ok := seen[target-x]; ok { fmt.Println(j, i); return }\n        seen[x] = i\n    }\n}'
    ),
    '数组,哈希表'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '两数之和');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '有效括号',
    '输入一个只包含 ()[]{} 的字符串。判断括号是否按正确顺序闭合，正确输出 true，否则输出 false。空字符串视为 true。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\nimport java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        String s = new BufferedReader(new InputStreamReader(System.in)).readLine();\n        // TODO: 输出 true 或 false\n    }\n}',
        'python', 'def solve():\n    s = input().strip()\n    # TODO: 输出 true 或 false\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst s = fs.readFileSync(0, "utf8").trim();\n// TODO: 输出 true 或 false',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    // TODO: 输出 true 或 false\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    // TODO: 输出 true 或 false\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '()[]{}', 'expected', 'true'),
        JSON_OBJECT('input', '([)]', 'expected', 'false'),
        JSON_OBJECT('input', '{[]}', 'expected', 'true')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; import java.util.*; public class Solution { public static void main(String[] args) throws Exception { String s = new BufferedReader(new InputStreamReader(System.in)).readLine(); if (s == null) s = ""; Map<Character,Character> map = new HashMap<>(); map.put(41, 40); map.put(93, 91); map.put(125, 123); Deque<Character> st = new ArrayDeque<>(); for (char c : s.toCharArray()) { if (map.containsValue(c)) st.push(c); else if (map.containsKey(c)) { if (st.isEmpty() || st.pop() != map.get(c)) { System.out.println("false"); return; } } } System.out.println(st.isEmpty() ? "true" : "false"); } }',
        'python', 'def solve():\n    s = input().strip()\n    pairs = {")": "(", "]": "[", "}": "{"}\n    stack = []\n    for ch in s:\n        if ch in "([{":\n            stack.append(ch)\n        elif not stack or stack.pop() != pairs[ch]:\n            print("false")\n            return\n    print("true" if not stack else "false")\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const s = fs.readFileSync(0, "utf8").trim(); const pairs = {")":"(", "]":"[", "}":"{"}; const stack = []; for (const ch of s) { if ("([{".includes(ch)) stack.push(ch); else if (!stack.length || stack.pop() !== pairs[ch]) { console.log("false"); process.exit(0); } } console.log(stack.length === 0 ? "true" : "false");',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    stack<char> st;\n    for (char c : s) {\n        if (c == 40 || c == 91 || c == 123) st.push(c);\n        else if (st.empty()) { cout << "false"; return 0; }\n        else if (c == 41 && st.top() != 40) { cout << "false"; return 0; }\n        else if (c == 93 && st.top() != 91) { cout << "false"; return 0; }\n        else if (c == 125 && st.top() != 123) { cout << "false"; return 0; }\n        else st.pop();\n    }\n    cout << (st.empty() ? "true" : "false");\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    pairs := map[byte]byte{\')\': \'(\', \']\': \'[\', \'}\': \'{\'}\n    var st []byte\n    for i := 0; i < len(s); i++ {\n        c := s[i]\n        if c == \'(\' || c == \'[\' || c == \'{\' { st = append(st, c); continue }\n        if len(st) == 0 || st[len(st)-1] != pairs[c] { fmt.Print("false"); return }\n        st = st[:len(st)-1]\n    }\n    if len(st) == 0 { fmt.Print("true") } else { fmt.Print("false") }\n}'
    ),
    '栈,字符串'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '有效括号');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '无重复字符的最长子串',
    '输入一个字符串 s，输出不含重复字符的最长连续子串长度。字符串可能包含大小写字母、数字和常见符号。',
    'medium',
    JSON_OBJECT(
        'java', 'import java.io.*;\nimport java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        String s = new BufferedReader(new InputStreamReader(System.in)).readLine();\n        // TODO: 输出最长长度\n    }\n}',
        'python', 'def solve():\n    s = input().strip()\n    # TODO: 输出最长长度\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst s = fs.readFileSync(0, "utf8").trim();\n// TODO: 输出最长长度',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    // TODO: 输出最长长度\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    // TODO: 输出最长长度\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', 'abcabcbb', 'expected', '3'),
        JSON_OBJECT('input', 'bbbbb', 'expected', '1'),
        JSON_OBJECT('input', 'pwwkew', 'expected', '3')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; import java.util.*; public class Solution { public static void main(String[] args) throws Exception { String s = new BufferedReader(new InputStreamReader(System.in)).readLine(); if (s == null) s = ""; Map<Character,Integer> last = new HashMap<>(); int left = 0, ans = 0; for (int r = 0; r < s.length(); r++) { char c = s.charAt(r); if (last.containsKey(c) && last.get(c) >= left) left = last.get(c) + 1; last.put(c, r); ans = Math.max(ans, r - left + 1); } System.out.println(ans); } }',
        'python', 'def solve():\n    s = input().strip()\n    last = {}\n    left = ans = 0\n    for right, ch in enumerate(s):\n        if ch in last and last[ch] >= left:\n            left = last[ch] + 1\n        last[ch] = right\n        ans = max(ans, right - left + 1)\n    print(ans)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const s = fs.readFileSync(0, "utf8").trim(); const last = new Map(); let left = 0, ans = 0; for (let r = 0; r < s.length; r++) { const ch = s[r]; if (last.has(ch) && last.get(ch) >= left) left = last.get(ch) + 1; last.set(ch, r); ans = Math.max(ans, r - left + 1); } console.log(ans);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    unordered_map<char,int> last;\n    int left = 0, ans = 0;\n    for (int r = 0; r < (int)s.size(); r++) {\n        if (last.count(s[r]) && last[s[r]] >= left) left = last[s[r]] + 1;\n        last[s[r]] = r;\n        ans = max(ans, r - left + 1);\n    }\n    cout << ans;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    last := make(map[rune]int)\n    left, ans := 0, 0\n    for r, ch := range s {\n        if prev, ok := last[ch]; ok && prev >= left { left = prev + 1 }\n        last[ch] = r\n        if r-left+1 > ans { ans = r - left + 1 }\n    }\n    fmt.Print(ans)\n}'
    ),
    '滑动窗口,哈希表'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '无重复字符的最长子串');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '最大子数组和',
    '输入第一行是 n，第二行是 n 个整数。请输出一个连续子数组能取得的最大和。数组至少包含一个元素。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        String[] parts = br.readLine().trim().split("\\\\s+");\n        // TODO: 输出最大子数组和\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    # TODO: 输出最大子数组和\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst n = Number(lines[0]);\nconst nums = lines[1].trim().split(/\\s+/).map(Number);\n// TODO: 输出最大子数组和',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    // TODO: 输出最大子数组和\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    // TODO: 输出最大子数组和\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '9\n-2 1 -3 4 -1 2 1 -5 4', 'expected', '6'),
        JSON_OBJECT('input', '1\n5', 'expected', '5'),
        JSON_OBJECT('input', '5\n5 4 -1 7 8', 'expected', '23')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); String[] p = br.readLine().trim().split("\\\\s+"); int cur = Integer.parseInt(p[0]), ans = cur; for (int i = 1; i < n; i++) { int x = Integer.parseInt(p[i]); cur = Math.max(x, cur + x); ans = Math.max(ans, cur); } System.out.println(ans); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    cur = ans = nums[0]\n    for x in nums[1:]:\n        cur = max(x, cur + x)\n        ans = max(ans, cur)\n    print(ans)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const n = Number(lines[0]); const nums = lines[1].trim().split(/\\s+/).map(Number); let cur = nums[0], ans = nums[0]; for (let i = 1; i < n; i++) { cur = Math.max(nums[i], cur + nums[i]); ans = Math.max(ans, cur); } console.log(ans);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    int cur = nums[0], ans = nums[0];\n    for (int i = 1; i < n; i++) { cur = max(nums[i], cur + nums[i]); ans = max(ans, cur); }\n    cout << ans;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    cur, ans := nums[0], nums[0]\n    for i := 1; i < n; i++ {\n        if nums[i] > cur+nums[i] { cur = nums[i] } else { cur = cur + nums[i] }\n        if cur > ans { ans = cur }\n    }\n    fmt.Print(ans)\n}'
    ),
    '动态规划,数组'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '最大子数组和');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '合并区间',
    '输入第一行是区间数量 n，接下来 n 行每行两个整数 l r。请合并所有重叠区间，按左端点升序输出；每个区间一行，格式为 l r。',
    'medium',
    JSON_OBJECT(
        'java', 'import java.io.*;\nimport java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        // TODO: 读取区间并输出合并结果\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    intervals = [list(map(int, input().split())) for _ in range(n)]\n    # TODO: 输出合并结果\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst n = Number(lines[0]);\n// TODO: 读取区间并输出合并结果',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    // TODO: 读取区间并输出合并结果\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    // TODO: 读取区间并输出合并结果\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '4\n1 3\n2 6\n8 10\n15 18', 'expected', '1 6\n8 10\n15 18'),
        JSON_OBJECT('input', '2\n1 4\n4 5', 'expected', '1 5'),
        JSON_OBJECT('input', '3\n5 7\n1 2\n2 3', 'expected', '1 3\n5 7')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; import java.util.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); int[][] a = new int[n][2]; for (int i = 0; i < n; i++) { String[] p = br.readLine().trim().split("\\\\s+"); a[i][0] = Integer.parseInt(p[0]); a[i][1] = Integer.parseInt(p[1]); } Arrays.sort(a, Comparator.comparingInt(x -> x[0])); StringBuilder out = new StringBuilder(); int l = a[0][0], r = a[0][1]; for (int i = 1; i < n; i++) { if (a[i][0] <= r) r = Math.max(r, a[i][1]); else { out.append(l).append(" ").append(r).append("\\n"); l = a[i][0]; r = a[i][1]; } } out.append(l).append(" ").append(r); System.out.print(out); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    intervals = [list(map(int, input().split())) for _ in range(n)]\n    intervals.sort()\n    ans = []\n    for l, r in intervals:\n        if not ans or l > ans[-1][1]:\n            ans.append([l, r])\n        else:\n            ans[-1][1] = max(ans[-1][1], r)\n    print("\\n".join(f"{l} {r}" for l, r in ans))\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const n = Number(lines[0]); const arr = []; for (let i = 1; i <= n; i++) arr.push(lines[i].trim().split(/\\s+/).map(Number)); arr.sort((a,b) => a[0] - b[0]); const ans = []; for (const [l,r] of arr) { if (!ans.length || l > ans[ans.length-1][1]) ans.push([l,r]); else ans[ans.length-1][1] = Math.max(ans[ans.length-1][1], r); } console.log(ans.map(x => x.join(" ")).join("\\n"));',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<pair<int,int>> a(n);\n    for (int i = 0; i < n; i++) cin >> a[i].first >> a[i].second;\n    sort(a.begin(), a.end());\n    int l = a[0].first, r = a[0].second;\n    for (int i = 1; i < n; i++) {\n        if (a[i].first <= r) r = max(r, a[i].second);\n        else { cout << l << " " << r << "\\n"; l = a[i].first; r = a[i].second; }\n    }\n    cout << l << " " << r;\n    return 0;\n}',
        'go', 'package main\nimport ("fmt"; "sort")\nfunc main() {\n    var n int; fmt.Scan(&n)\n    a := make([][2]int, n)\n    for i := range a { fmt.Scan(&a[i][0], &a[i][1]) }\n    sort.Slice(a, func(i, j int) bool { return a[i][0] < a[j][0] })\n    l, r := a[0][0], a[0][1]\n    for i := 1; i < n; i++ {\n        if a[i][0] <= r { if a[i][1] > r { r = a[i][1] } } else { fmt.Println(l, r); l, r = a[i][0], a[i][1] }\n    }\n    fmt.Println(l, r)\n}'
    ),
    '排序,区间'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '合并区间');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '爬楼梯',
    '一次可以爬 1 级或 2 级台阶。输入台阶数 n，输出爬到楼顶的不同方法数。n 的范围为 1 到 45。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        int n = Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine().trim());\n        // TODO: 输出方法数\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    # TODO: 输出方法数\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst n = Number(fs.readFileSync(0, "utf8").trim());\n// TODO: 输出方法数',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    // TODO: 输出方法数\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    // TODO: 输出方法数\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '2', 'expected', '2'),
        JSON_OBJECT('input', '3', 'expected', '3'),
        JSON_OBJECT('input', '5', 'expected', '8')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { int n = Integer.parseInt(new BufferedReader(new InputStreamReader(System.in)).readLine().trim()); long a = 1, b = 1; for (int i = 1; i <= n; i++) { long c = a + b; a = b; b = c; } System.out.println(a); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    a = b = 1\n    for _ in range(n):\n        a, b = b, a + b\n    print(a)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const n = Number(fs.readFileSync(0, "utf8").trim()); let a = 1, b = 1; for (let i = 0; i < n; i++) [a, b] = [b, a + b]; console.log(a);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    long long a = 1, b = 1;\n    for (int i = 1; i <= n; i++) { long long c = a + b; a = b; b = c; }\n    cout << a;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    a, b := 1, 1\n    for i := 0; i < n; i++ { a, b = b, a+b }\n    fmt.Print(a)\n}'
    ),
    '动态规划'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '爬楼梯');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '搜索插入位置',
    '输入第一行是升序数组长度 n，第二行是 n 个严格升序整数，第三行是目标值 target。若 target 存在，输出下标；否则输出它应插入的位置。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        String[] parts = br.readLine().trim().split("\\\\s+");\n        int target = Integer.parseInt(br.readLine().trim());\n        // TODO: 输出下标\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    target = int(input().strip())\n    # TODO: 输出下标\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst n = Number(lines[0]);\nconst nums = lines[1].trim().split(/\\s+/).map(Number);\nconst target = Number(lines[2]);\n// TODO: 输出下标',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    int target; cin >> target;\n    // TODO: 输出下标\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    var target int; fmt.Scan(&target)\n    // TODO: 输出下标\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '4\n1 3 5 6\n5', 'expected', '2'),
        JSON_OBJECT('input', '4\n1 3 5 6\n2', 'expected', '1'),
        JSON_OBJECT('input', '4\n1 3 5 6\n7', 'expected', '4')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); String[] p = br.readLine().trim().split("\\\\s+"); int target = Integer.parseInt(br.readLine().trim()); int l = 0, r = n; while (l < r) { int m = (l + r) >>> 1; if (Integer.parseInt(p[m]) < target) l = m + 1; else r = m; } System.out.println(l); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    nums = list(map(int, input().split()))\n    target = int(input().strip())\n    l, r = 0, n\n    while l < r:\n        m = (l + r) // 2\n        if nums[m] < target:\n            l = m + 1\n        else:\n            r = m\n    print(l)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const n = Number(lines[0]); const nums = lines[1].trim().split(/\\s+/).map(Number); const target = Number(lines[2]); let l = 0, r = n; while (l < r) { const m = Math.floor((l + r) / 2); if (nums[m] < target) l = m + 1; else r = m; } console.log(l);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> nums(n);\n    for (int i = 0; i < n; i++) cin >> nums[i];\n    int target; cin >> target;\n    int l = 0, r = n;\n    while (l < r) { int m = (l + r) >> 1; if (nums[m] < target) l = m + 1; else r = m; }\n    cout << l;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    nums := make([]int, n)\n    for i := range nums { fmt.Scan(&nums[i]) }\n    var target int; fmt.Scan(&target)\n    l, r := 0, n\n    for l < r { m := (l + r) / 2; if nums[m] < target { l = m + 1 } else { r = m } }\n    fmt.Print(l)\n}'
    ),
    '二分查找'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '搜索插入位置');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '最长公共前缀',
    '输入第一行是字符串数量 n，接下来 n 行每行一个字符串。输出这些字符串的最长公共前缀；若不存在公共前缀，输出空行。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        // TODO: 输出最长公共前缀\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    words = [input().strip() for _ in range(n)]\n    # TODO: 输出最长公共前缀\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").split(/\\n/);\nconst n = Number(lines[0]);\nconst words = lines.slice(1, 1 + n).map(s => s.trim());\n// TODO: 输出最长公共前缀',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<string> words(n);\n    for (int i = 0; i < n; i++) cin >> words[i];\n    // TODO: 输出最长公共前缀\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    words := make([]string, n)\n    for i := range words { fmt.Scan(&words[i]) }\n    // TODO: 输出最长公共前缀\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '3\nflower\nflow\nflight', 'expected', 'fl'),
        JSON_OBJECT('input', '3\ndog\nracecar\ncar', 'expected', ''),
        JSON_OBJECT('input', '2\ninterview\ninternet', 'expected', 'inte')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); String prefix = n > 0 ? br.readLine().trim() : ""; for (int i = 1; i < n; i++) { String s = br.readLine().trim(); while (!s.startsWith(prefix)) prefix = prefix.substring(0, prefix.length() - 1); } System.out.println(prefix); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    words = [input().strip() for _ in range(n)]\n    prefix = words[0] if words else ""\n    for w in words[1:]:\n        while not w.startswith(prefix):\n            prefix = prefix[:-1]\n    print(prefix)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").split(/\\n/); const n = Number(lines[0]); const words = lines.slice(1, 1 + n).map(s => s.trim()); let prefix = words[0] || ""; for (const w of words.slice(1)) { while (!w.startsWith(prefix)) prefix = prefix.slice(0, -1); } console.log(prefix);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<string> words(n);\n    for (int i = 0; i < n; i++) cin >> words[i];\n    string prefix = words[0];\n    for (int i = 1; i < n; i++) while (words[i].find(prefix) != 0) prefix.pop_back();\n    cout << prefix;\n    return 0;\n}',
        'go', 'package main\nimport ("fmt"; "strings")\nfunc main() {\n    var n int; fmt.Scan(&n)\n    words := make([]string, n)\n    for i := range words { fmt.Scan(&words[i]) }\n    prefix := words[0]\n    for _, w := range words[1:] { for !strings.HasPrefix(w, prefix) { prefix = prefix[:len(prefix)-1] } }\n    fmt.Print(prefix)\n}'
    ),
    '字符串'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '最长公共前缀');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '盛水最多容器',
    '输入第一行是 n，第二行是 n 个非负整数 height。每个数表示一条竖线高度，请输出两条线能围成的最大面积。',
    'medium',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        int n = Integer.parseInt(br.readLine().trim());\n        String[] parts = br.readLine().trim().split("\\\\s+");\n        // TODO: 输出最大面积\n    }\n}',
        'python', 'def solve():\n    n = int(input().strip())\n    height = list(map(int, input().split()))\n    # TODO: 输出最大面积\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst n = Number(lines[0]);\nconst height = lines[1].trim().split(/\\s+/).map(Number);\n// TODO: 输出最大面积',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> h(n);\n    for (int i = 0; i < n; i++) cin >> h[i];\n    // TODO: 输出最大面积\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    h := make([]int, n)\n    for i := range h { fmt.Scan(&h[i]) }\n    // TODO: 输出最大面积\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '9\n1 8 6 2 5 4 8 3 7', 'expected', '49'),
        JSON_OBJECT('input', '2\n1 1', 'expected', '1'),
        JSON_OBJECT('input', '5\n4 3 2 1 4', 'expected', '16')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); int n = Integer.parseInt(br.readLine().trim()); String[] p = br.readLine().trim().split("\\\\s+"); int l = 0, r = n - 1, ans = 0; while (l < r) { int hl = Integer.parseInt(p[l]), hr = Integer.parseInt(p[r]); ans = Math.max(ans, (r - l) * Math.min(hl, hr)); if (hl < hr) l++; else r--; } System.out.println(ans); } }',
        'python', 'def solve():\n    n = int(input().strip())\n    h = list(map(int, input().split()))\n    l, r, ans = 0, n - 1, 0\n    while l < r:\n        ans = max(ans, (r - l) * min(h[l], h[r]))\n        if h[l] < h[r]:\n            l += 1\n        else:\n            r -= 1\n    print(ans)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const n = Number(lines[0]); const h = lines[1].trim().split(/\\s+/).map(Number); let l = 0, r = n - 1, ans = 0; while (l < r) { ans = Math.max(ans, (r - l) * Math.min(h[l], h[r])); if (h[l] < h[r]) l++; else r--; } console.log(ans);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int n; cin >> n;\n    vector<int> h(n);\n    for (int i = 0; i < n; i++) cin >> h[i];\n    int l = 0, r = n - 1, ans = 0;\n    while (l < r) { ans = max(ans, (r - l) * min(h[l], h[r])); if (h[l] < h[r]) l++; else r--; }\n    cout << ans;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var n int; fmt.Scan(&n)\n    h := make([]int, n)\n    for i := range h { fmt.Scan(&h[i]) }\n    l, r, ans := 0, n-1, 0\n    for l < r {\n        w := r - l\n        if h[l] < h[r] { w = h[l] } else { w = h[r] }\n        if w*(r-l) > ans { ans = w * (r - l) }\n        if h[l] < h[r] { l++ } else { r-- }\n    }\n    fmt.Print(ans)\n}'
    ),
    '双指针,数组'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '盛水最多容器');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '最小路径和',
    '输入第一行是 m n，接下来 m 行每行 n 个非负整数，表示网格。每次只能向右或向下移动，请输出从左上角到右下角的最小路径和。',
    'medium',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        String[] size = br.readLine().trim().split("\\\\s+");\n        // TODO: 读取网格并输出最小路径和\n    }\n}',
        'python', 'def solve():\n    m, n = map(int, input().split())\n    grid = [list(map(int, input().split())) for _ in range(m)]\n    # TODO: 输出最小路径和\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst lines = fs.readFileSync(0, "utf8").trim().split(/\\n/);\nconst [m, n] = lines[0].trim().split(/\\s+/).map(Number);\n// TODO: 读取网格并输出最小路径和',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int m, n; cin >> m >> n;\n    // TODO: 读取网格并输出最小路径和\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var m, n int; fmt.Scan(&m, &n)\n    // TODO: 读取网格并输出最小路径和\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', '3 3\n1 3 1\n1 5 1\n4 2 1', 'expected', '7'),
        JSON_OBJECT('input', '2 3\n1 2 3\n4 5 6', 'expected', '12'),
        JSON_OBJECT('input', '1 1\n5', 'expected', '5')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); String[] size = br.readLine().trim().split("\\\\s+"); int m = Integer.parseInt(size[0]), n = Integer.parseInt(size[1]); int[] dp = new int[n]; for (int i = 0; i < m; i++) { String[] row = br.readLine().trim().split("\\\\s+"); for (int j = 0; j < n; j++) { int x = Integer.parseInt(row[j]); if (i == 0 && j == 0) dp[j] = x; else if (i == 0) dp[j] = dp[j - 1] + x; else if (j == 0) dp[j] = dp[j] + x; else dp[j] = Math.min(dp[j], dp[j - 1]) + x; } } System.out.println(dp[n - 1]); } }',
        'python', 'def solve():\n    m, n = map(int, input().split())\n    dp = [0] * n\n    for i in range(m):\n        row = list(map(int, input().split()))\n        for j, x in enumerate(row):\n            if i == 0 and j == 0:\n                dp[j] = x\n            elif i == 0:\n                dp[j] = dp[j - 1] + x\n            elif j == 0:\n                dp[j] += x\n            else:\n                dp[j] = min(dp[j], dp[j - 1]) + x\n    print(dp[-1])\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const lines = fs.readFileSync(0, "utf8").trim().split(/\\n/); const [m,n] = lines[0].trim().split(/\\s+/).map(Number); const dp = Array(n).fill(0); for (let i = 0; i < m; i++) { const row = lines[i+1].trim().split(/\\s+/).map(Number); for (let j = 0; j < n; j++) { const x = row[j]; if (i === 0 && j === 0) dp[j] = x; else if (i === 0) dp[j] = dp[j-1] + x; else if (j === 0) dp[j] += x; else dp[j] = Math.min(dp[j], dp[j-1]) + x; } } console.log(dp[n-1]);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    int m, n; cin >> m >> n;\n    vector<int> dp(n);\n    for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) {\n        int x; cin >> x;\n        if (i == 0 && j == 0) dp[j] = x;\n        else if (i == 0) dp[j] = dp[j-1] + x;\n        else if (j == 0) dp[j] += x;\n        else dp[j] = min(dp[j], dp[j-1]) + x;\n    }\n    cout << dp[n-1];\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var m, n int; fmt.Scan(&m, &n)\n    dp := make([]int, n)\n    for i := 0; i < m; i++ { for j := 0; j < n; j++ {\n        var x int; fmt.Scan(&x)\n        if i == 0 && j == 0 { dp[j] = x } else if i == 0 { dp[j] = dp[j-1] + x } else if j == 0 { dp[j] += x } else { if dp[j] > dp[j-1] { dp[j] = dp[j-1] + x } else { dp[j] += x } }\n    }}\n    fmt.Print(dp[n-1])\n}'
    ),
    '动态规划,矩阵'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '最小路径和');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '最长公共子序列',
    '输入两行字符串 text1 和 text2。输出两个字符串的最长公共子序列长度。子序列不要求连续，但相对顺序不能改变。',
    'medium',
    JSON_OBJECT(
        'java', 'import java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n        String a = br.readLine();\n        String b = br.readLine();\n        // TODO: 输出最长公共子序列长度\n    }\n}',
        'python', 'def solve():\n    a = input().strip()\n    b = input().strip()\n    # TODO: 输出最长公共子序列长度\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst [a, b] = fs.readFileSync(0, "utf8").trim().split(/\\n/).map(s => s.trim());\n// TODO: 输出最长公共子序列长度',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string a, b; cin >> a >> b;\n    // TODO: 输出最长公共子序列长度\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var a, b string; fmt.Scan(&a, &b)\n    // TODO: 输出最长公共子序列长度\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', 'abcde\nace', 'expected', '3'),
        JSON_OBJECT('input', 'abc\nabc', 'expected', '3'),
        JSON_OBJECT('input', 'abc\ndef', 'expected', '0')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; public class Solution { public static void main(String[] args) throws Exception { BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); String a = br.readLine(), b = br.readLine(); int[] dp = new int[b.length() + 1]; for (int i = 1; i <= a.length(); i++) { int prev = 0; for (int j = 1; j <= b.length(); j++) { int tmp = dp[j]; if (a.charAt(i - 1) == b.charAt(j - 1)) dp[j] = prev + 1; else dp[j] = Math.max(dp[j], dp[j - 1]); prev = tmp; } } System.out.println(dp[b.length()]); } }',
        'python', 'def solve():\n    a = input().strip()\n    b = input().strip()\n    dp = [0] * (len(b) + 1)\n    for ca in a:\n        prev = 0\n        for j, cb in enumerate(b, 1):\n            tmp = dp[j]\n            dp[j] = prev + 1 if ca == cb else max(dp[j], dp[j - 1])\n            prev = tmp\n    print(dp[-1])\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const [a,b] = fs.readFileSync(0, "utf8").trim().split(/\\n/).map(s => s.trim()); const dp = Array(b.length + 1).fill(0); for (const ca of a) { let prev = 0; for (let j = 1; j <= b.length; j++) { const tmp = dp[j]; dp[j] = ca === b[j-1] ? prev + 1 : Math.max(dp[j], dp[j-1]); prev = tmp; } } console.log(dp[b.length]);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string a, b; cin >> a >> b;\n    vector<int> dp(b.size() + 1);\n    for (char ca : a) { int prev = 0; for (int j = 1; j <= (int)b.size(); j++) { int tmp = dp[j]; dp[j] = (ca == b[j-1]) ? prev + 1 : max(dp[j], dp[j-1]); prev = tmp; } }\n    cout << dp[b.size()];\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var a, b string; fmt.Scan(&a, &b)\n    dp := make([]int, len(b)+1)\n    for _, ca := range a { prev := 0; for j := 1; j <= len(b); j++ { tmp := dp[j]; if ca == rune(b[j-1]) { dp[j] = prev + 1 } else if dp[j] < dp[j-1] { dp[j] = dp[j-1] }; prev = tmp } }\n    fmt.Print(dp[len(b)])\n}'
    ),
    '动态规划,字符串'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '最长公共子序列');

INSERT INTO algorithm_problem (title, description, difficulty, starter_code, test_cases, solution_code, category)
SELECT
    '罗马数字转整数',
    '输入一个合法罗马数字字符串，字符只包含 I、V、X、L、C、D、M，范围为 1 到 3999。输出对应整数。',
    'easy',
    JSON_OBJECT(
        'java', 'import java.io.*;\nimport java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) throws Exception {\n        String s = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();\n        // TODO: 输出整数\n    }\n}',
        'python', 'def solve():\n    s = input().strip()\n    # TODO: 输出整数\n\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs");\nconst s = fs.readFileSync(0, "utf8").trim();\n// TODO: 输出整数',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    // TODO: 输出整数\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    // TODO: 输出整数\n}'
    ),
    JSON_ARRAY(
        JSON_OBJECT('input', 'III', 'expected', '3'),
        JSON_OBJECT('input', 'LVIII', 'expected', '58'),
        JSON_OBJECT('input', 'MCMXCIV', 'expected', '1994')
    ),
    JSON_OBJECT(
        'java', 'import java.io.*; import java.util.*; public class Solution { public static void main(String[] args) throws Exception { String s = new BufferedReader(new InputStreamReader(System.in)).readLine().trim(); Map<Character,Integer> v = new HashMap<>() {{ put(73,1); put(86,5); put(88,10); put(76,50); put(67,100); put(68,500); put(77,1000); }}; int ans = 0; for (int i = 0; i < s.length(); i++) { int cur = v.get(s.charAt(i)); int next = i + 1 < s.length() ? v.get(s.charAt(i + 1)) : 0; ans += cur < next ? -cur : cur; } System.out.println(ans); } }',
        'python', 'def solve():\n    s = input().strip()\n    v = {"I":1,"V":5,"X":10,"L":50,"C":100,"D":500,"M":1000}\n    ans = 0\n    for i, ch in enumerate(s):\n        ans += -v[ch] if i + 1 < len(s) and v[ch] < v[s[i + 1]] else v[ch]\n    print(ans)\nif __name__ == "__main__":\n    solve()',
        'javascript', 'const fs = require("fs"); const s = fs.readFileSync(0, "utf8").trim(); const v = {I:1,V:5,X:10,L:50,C:100,D:500,M:1000}; let ans = 0; for (let i = 0; i < s.length; i++) ans += v[s[i]] < (v[s[i+1]] || 0) ? -v[s[i]] : v[s[i]]; console.log(ans);',
        'cpp', '#include <bits/stdc++.h>\nusing namespace std;\nint main() {\n    string s; cin >> s;\n    unordered_map<char,int> v = {{73,1},{86,5},{88,10},{76,50},{67,100},{68,500},{77,1000}};\n    int ans = 0;\n    for (int i = 0; i < (int)s.size(); i++) { int cur = v[s[i]], nxt = i+1 < (int)s.size() ? v[s[i+1]] : 0; ans += cur < nxt ? -cur : cur; }\n    cout << ans;\n    return 0;\n}',
        'go', 'package main\nimport "fmt"\nfunc main() {\n    var s string; fmt.Scan(&s)\n    v := map[byte]int{73:1,86:5,88:10,76:50,67:100,68:500,77:1000}\n    ans := 0\n    for i := 0; i < len(s); i++ { cur := v[s[i]]; nxt := 0; if i+1 < len(s) { nxt = v[s[i+1] }; if cur < nxt { ans -= cur } else { ans += cur } }\n    fmt.Print(ans)\n}'
    ),
    '字符串,模拟'
WHERE NOT EXISTS (SELECT 1 FROM algorithm_problem WHERE title = '罗马数字转整数');

-- ==================== 八股文扩充 ====================
INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机网络' ORDER BY id LIMIT 1), 1,
       'TCP 为什么需要四次挥手而不是三次挥手？',
       '[{"label":"A","content":"因为 TCP 必须固定发送四个包"},{"label":"B","content":"因为连接关闭是双向的，双方都要分别发送 FIN 和 ACK"},{"label":"C","content":"因为 HTTP 要求四次挥手"},{"label":"D","content":"因为服务端不能主动关闭连接"}]',
       'B',
       'TCP 是全双工连接。主动关闭方发送 FIN 后，只表示自己不再发送数据；被动关闭方仍可能有数据要发，因此需要先 ACK，再等数据发完后发送自己的 FIN，最终由主动关闭方 ACK。',
       2,
       'TCP,四次挥手,网络'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'TCP 为什么需要四次挥手而不是三次挥手？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机网络' ORDER BY id LIMIT 1), 2,
       'HTTP/2 相比 HTTP/1.1 的典型改进包括哪些？',
       '[{"label":"A","content":"二进制分帧"},{"label":"B","content":"多路复用"},{"label":"C","content":"头部压缩"},{"label":"D","content":"彻底移除 TCP"}]',
       'A,B,C',
       'HTTP/2 引入二进制分帧、多路复用、HPACK 头部压缩和服务端推送等能力，但它通常仍运行在 TCP 之上。',
       2,
       'HTTP2,网络'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'HTTP/2 相比 HTTP/1.1 的典型改进包括哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机网络' ORDER BY id LIMIT 1), 1,
       '浏览器输入 URL 后，最先发生的网络相关步骤通常是？',
       '[{"label":"A","content":"DNS 解析域名"},{"label":"B","content":"渲染 DOM 树"},{"label":"C","content":"执行 JavaScript"},{"label":"D","content":"绘制 CSSOM"}]',
       'A',
       '需要先把域名解析为 IP 地址，随后才会建立 TCP/TLS 连接并发送 HTTP 请求。DOM、CSSOM 和 JS 执行发生在响应资源返回之后。',
       1,
       'DNS,浏览器,网络'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '浏览器输入 URL 后，最先发生的网络相关步骤通常是？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机网络' ORDER BY id LIMIT 1), 3,
       'UDP 一定比 TCP 不安全，因此不能用于实时音视频。',
       NULL,
       '错误',
       'UDP 不提供 TCP 那样的可靠传输和有序交付，但实时音视频更看重低延迟，常在 UDP 之上实现重传、拥塞控制或加密，例如 QUIC/WebRTC 的部分场景。',
       1,
       'UDP,TCP,实时通信'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'UDP 一定比 TCP 不安全，因此不能用于实时音视频。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '操作系统' ORDER BY id LIMIT 1), 1,
       '线程上下文切换通常需要保存和恢复哪些信息？',
       '[{"label":"A","content":"寄存器、程序计数器、栈指针等执行现场"},{"label":"B","content":"显示器分辨率"},{"label":"C","content":"源代码文件路径"},{"label":"D","content":"数据库索引页"}]',
       'A',
       '线程上下文切换需要保存当前线程的 CPU 执行现场，并恢复下一个线程的执行现场。频繁切换会带来调度和缓存失效开销。',
       2,
       '线程,上下文切换'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '线程上下文切换通常需要保存和恢复哪些信息？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '操作系统' ORDER BY id LIMIT 1), 2,
       '常见的页面置换算法有哪些？',
       '[{"label":"A","content":"FIFO"},{"label":"B","content":"LRU"},{"label":"C","content":"Clock"},{"label":"D","content":"BFS"}]',
       'A,B,C',
       'FIFO、LRU、Clock 都是常见页面置换算法。BFS 是图搜索算法，不是页面置换算法。',
       2,
       '虚拟内存,页面置换'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '常见的页面置换算法有哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '操作系统' ORDER BY id LIMIT 1), 1,
       '用户态和内核态的主要区别是什么？',
       '[{"label":"A","content":"权限级别不同，内核态可以执行特权指令"},{"label":"B","content":"用户态一定更快且能访问所有内存"},{"label":"C","content":"内核态不能访问硬件"},{"label":"D","content":"两者没有区别"}]',
       'A',
       '用户态权限受限，不能直接执行特权指令或随意访问内核内存；系统调用、中断等会进入内核态，由内核完成受保护的操作。',
       1,
       '用户态,内核态'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '用户态和内核态的主要区别是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '操作系统' ORDER BY id LIMIT 1), 3,
       '死锁只要出现循环等待就一定会发生。',
       NULL,
       '错误',
       '循环等待是死锁的必要条件之一，但还需要互斥、请求与保持、不可剥夺等条件同时满足。破坏任一必要条件都可能避免死锁。',
       2,
       '死锁,并发'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '死锁只要出现循环等待就一定会发生。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据结构' ORDER BY id LIMIT 1), 1,
       '红黑树相对普通二叉搜索树的优势是什么？',
       '[{"label":"A","content":"通过近似平衡保证操作复杂度为 O(log n)"},{"label":"B","content":"所有操作都变成 O(1)"},{"label":"C","content":"不需要比较大小"},{"label":"D","content":"只能存储整数"}]',
       'A',
       '红黑树通过颜色和旋转规则保持近似平衡，避免普通 BST 在有序输入下退化成链表，查找、插入、删除通常为 O(log n)。',
       2,
       '红黑树,BST'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '红黑树相对普通二叉搜索树的优势是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据结构' ORDER BY id LIMIT 1), 2,
       '哈希冲突的常见解决方式有哪些？',
       '[{"label":"A","content":"链地址法"},{"label":"B","content":"开放寻址法"},{"label":"C","content":"再哈希"},{"label":"D","content":"直接丢弃冲突元素"}]',
       'A,B,C',
       '链地址法、开放寻址法和再哈希都可处理冲突。直接丢弃元素会破坏数据完整性，不属于正确解决方式。',
       2,
       '哈希表,冲突'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '哈希冲突的常见解决方式有哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据结构' ORDER BY id LIMIT 1), 1,
       '优先队列通常可以用哪种数据结构高效实现？',
       '[{"label":"A","content":"堆"},{"label":"B","content":"普通数组且不排序"},{"label":"C","content":"单向链表且不维护顺序"},{"label":"D","content":"栈"}]',
       'A',
       '二叉堆可以在 O(log n) 时间插入和弹出优先级最高或最低的元素，是优先队列的常见实现。',
       1,
       '堆,优先队列'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '优先队列通常可以用哪种数据结构高效实现？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据结构' ORDER BY id LIMIT 1), 4,
       '并查集通常支持两个核心操作：查找根节点的 ____ 和合并集合的 ____。',
       NULL,
       'find,union',
       '并查集用 find 查询元素所在集合代表元，用 union 合并两个集合，常配合路径压缩和按秩合并优化。',
       2,
       '并查集'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '并查集通常支持两个核心操作：查找根节点的 ____ 和合并集合的 ____。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '算法' ORDER BY id LIMIT 1), 1,
       '动态规划中“状态转移方程”的作用是什么？',
       '[{"label":"A","content":"描述当前状态如何由更小子问题推导而来"},{"label":"B","content":"固定要求使用递归"},{"label":"C","content":"负责创建数据库表"},{"label":"D","content":"让算法变成随机算法"}]',
       'A',
       '状态转移方程是 DP 的核心，它把大问题拆成可复用的子问题，并描述状态之间的依赖关系。',
       2,
       '动态规划,状态转移'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '动态规划中“状态转移方程”的作用是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '算法' ORDER BY id LIMIT 1), 2,
       '判断一个问题是否适合二分查找，通常需要关注哪些条件？',
       '[{"label":"A","content":"搜索空间可以有序或具备单调性"},{"label":"B","content":"能够判断中间位置后舍弃一侧"},{"label":"C","content":"必须只能用于数组"},{"label":"D","content":"每次只能线性扫描"}]',
       'A,B',
       '二分的关键是单调性和可判定性，不一定只能用于数组，也可用于答案空间二分。',
       2,
       '二分查找,单调性'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '判断一个问题是否适合二分查找，通常需要关注哪些条件？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '算法' ORDER BY id LIMIT 1), 1,
       '回溯算法中的“剪枝”主要目的是什么？',
       '[{"label":"A","content":"提前排除不可能产生答案的搜索分支"},{"label":"B","content":"把所有分支都搜索两遍"},{"label":"C","content":"保证结果一定按字典序输出"},{"label":"D","content":"减少代码行数"}]',
       'A',
       '剪枝通过约束条件或上界估计提前停止无效分支，减少搜索空间，提高回溯算法效率。',
       2,
       '回溯,剪枝'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '回溯算法中的“剪枝”主要目的是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '算法' ORDER BY id LIMIT 1), 3,
       '快速排序在任何输入下的时间复杂度都是 O(n log n)。',
       NULL,
       '错误',
       '快速排序平均复杂度为 O(n log n)，但枢轴选择不当时最坏可退化为 O(n²)。随机化或三数取中可以降低退化概率。',
       2,
       '排序,快速排序'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '快速排序在任何输入下的时间复杂度都是 O(n log n)。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据库' ORDER BY id LIMIT 1), 1,
       'MySQL 联合索引 (a,b,c) 遵循的核心匹配原则是？',
       '[{"label":"A","content":"最左前缀原则"},{"label":"B","content":"随机匹配原则"},{"label":"C","content":"只看最后一列"},{"label":"D","content":"索引列越多越一定快"}]',
       'A',
       '联合索引按从左到右的列顺序组织，查询条件通常需要从最左列连续匹配才能充分利用索引。',
       2,
       'MySQL,联合索引'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'MySQL 联合索引 (a,b,c) 遵循的核心匹配原则是？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据库' ORDER BY id LIMIT 1), 2,
       '事务隔离级别过低可能导致哪些并发问题？',
       '[{"label":"A","content":"脏读"},{"label":"B","content":"不可重复读"},{"label":"C","content":"幻读"},{"label":"D","content":"语法错误"}]',
       'A,B,C',
       '隔离级别越低，越容易出现脏读、不可重复读、幻读等并发一致性问题。语法错误与事务隔离无关。',
       2,
       '事务,隔离级别'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '事务隔离级别过低可能导致哪些并发问题？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据库' ORDER BY id LIMIT 1), 1,
       '覆盖索引为什么能提升查询性能？',
       '[{"label":"A","content":"查询所需字段都在索引中，可减少回表"},{"label":"B","content":"它会禁用事务"},{"label":"C","content":"它会把所有数据放进内存"},{"label":"D","content":"它只适用于 DELETE"}]',
       'A',
       '覆盖索引包含查询需要的列，InnoDB 可以直接从二级索引返回结果，减少回表读取主键索引的成本。',
       2,
       '索引,覆盖索引'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '覆盖索引为什么能提升查询性能？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '数据库' ORDER BY id LIMIT 1), 3,
       'InnoDB 的主键索引叶子节点存储的是完整行数据。',
       NULL,
       '正确',
       'InnoDB 使用聚簇索引组织表数据，主键索引叶子节点保存完整行；二级索引叶子节点保存索引列和主键值。',
       2,
       'InnoDB,聚簇索引'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'InnoDB 的主键索引叶子节点存储的是完整行数据。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = 'Java' ORDER BY id LIMIT 1), 1,
       'HashMap 在 Java 8 中链表转红黑树的常见条件是什么？',
       '[{"label":"A","content":"链表长度超过阈值且数组容量达到一定大小"},{"label":"B","content":"任意插入都会转树"},{"label":"C","content":"只要 key 是字符串就转树"},{"label":"D","content":"负载因子小于 0.1"}]',
       'A',
       'Java 8 HashMap 通常在桶内链表长度超过 8 且数组容量至少 64 时树化，否则优先扩容。',
       2,
       'HashMap,Java集合'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'HashMap 在 Java 8 中链表转红黑树的常见条件是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = 'Java' ORDER BY id LIMIT 1), 2,
       'volatile 关键字的语义包括哪些？',
       '[{"label":"A","content":"保证可见性"},{"label":"B","content":"禁止相关指令重排序"},{"label":"C","content":"保证复合操作原子性"},{"label":"D","content":"让对象永久不被 GC"}]',
       'A,B',
       'volatile 保证变量写入对其他线程可见，并提供一定的有序性约束，但 i++ 这类复合操作仍不具备原子性。',
       2,
       'volatile,JMM'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'volatile 关键字的语义包括哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = 'Java' ORDER BY id LIMIT 1), 1,
       'Spring Bean 默认作用域是什么？',
       '[{"label":"A","content":"singleton"},{"label":"B","content":"prototype"},{"label":"C","content":"request"},{"label":"D","content":"session"}]',
       'A',
       'Spring 容器中 Bean 默认是 singleton，同一个容器内通常只有一个共享实例。prototype 会在每次获取时创建新实例。',
       1,
       'Spring,Bean作用域'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'Spring Bean 默认作用域是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = 'Java' ORDER BY id LIMIT 1), 3,
       'Java 的 synchronized 既可以修饰实例方法，也可以修饰静态方法。',
       NULL,
       '正确',
       '修饰实例方法时锁是当前对象 this；修饰静态方法时锁是 Class 对象。两者锁对象不同。',
       1,
       'synchronized,并发'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'Java 的 synchronized 既可以修饰实例方法，也可以修饰静态方法。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '设计模式' ORDER BY id LIMIT 1), 1,
       '策略模式主要解决什么问题？',
       '[{"label":"A","content":"把一组可替换算法封装起来，运行时选择"},{"label":"B","content":"保证全局只有一个对象"},{"label":"C","content":"隐藏远程调用细节"},{"label":"D","content":"把对象转换为 JSON"}]',
       'A',
       '策略模式把不同算法或行为封装为独立策略，使调用方依赖抽象，避免大量 if/else 分支，并支持运行时替换。',
       1,
       '策略模式'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '策略模式主要解决什么问题？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '设计模式' ORDER BY id LIMIT 1), 2,
       '代理模式常见用途包括哪些？',
       '[{"label":"A","content":"权限控制"},{"label":"B","content":"延迟加载"},{"label":"C","content":"远程代理"},{"label":"D","content":"改变 CPU 指令集"}]',
       'A,B,C',
       '代理模式通过代理对象控制对目标对象的访问，可用于权限、缓存、延迟加载、远程调用等场景。',
       2,
       '代理模式,AOP'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '代理模式常见用途包括哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '设计模式' ORDER BY id LIMIT 1), 1,
       '模板方法模式中的“不变流程”通常放在哪里？',
       '[{"label":"A","content":"抽象父类的方法中"},{"label":"B","content":"数据库触发器中"},{"label":"C","content":"随机数生成器中"},{"label":"D","content":"HTTP Header 中"}]',
       'A',
       '模板方法模式在父类定义算法骨架，把可变步骤延迟给子类实现，从而复用固定流程。',
       2,
       '模板方法'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '模板方法模式中的“不变流程”通常放在哪里？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '设计模式' ORDER BY id LIMIT 1), 3,
       '适配器模式的目标是让原本接口不兼容的对象可以协同工作。',
       NULL,
       '正确',
       '适配器模式通过包装或转换接口，使客户端可以按期望接口使用已有对象，常用于兼容老系统或第三方库。',
       1,
       '适配器模式'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '适配器模式的目标是让原本接口不兼容的对象可以协同工作。');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机组成原理' ORDER BY id LIMIT 1), 1,
       'CPU Cache 的主要作用是什么？',
       '[{"label":"A","content":"缓解 CPU 与主存速度差距"},{"label":"B","content":"替代硬盘永久存储"},{"label":"C","content":"提供网络路由"},{"label":"D","content":"编译源代码"}]',
       'A',
       'CPU Cache 位于 CPU 和主存之间，利用局部性原理缓存热点数据和指令，减少访问主存的延迟。',
       1,
       'Cache,局部性'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = 'CPU Cache 的主要作用是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机组成原理' ORDER BY id LIMIT 1), 2,
       '影响流水线性能的相关冲突包括哪些？',
       '[{"label":"A","content":"结构相关"},{"label":"B","content":"数据相关"},{"label":"C","content":"控制相关"},{"label":"D","content":"颜色相关"}]',
       'A,B,C',
       '指令流水线常见冲突包括结构相关、数据相关和控制相关。颜色相关不是组成原理中的流水线冲突类型。',
       2,
       '流水线,CPU'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '影响流水线性能的相关冲突包括哪些？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机组成原理' ORDER BY id LIMIT 1), 1,
       '补码表示法的一个重要优点是什么？',
       '[{"label":"A","content":"加减法可以统一用加法器处理"},{"label":"B","content":"只能表示正数"},{"label":"C","content":"会让内存容量翻倍"},{"label":"D","content":"不需要符号位"}]',
       'A',
       '补码把减法转化为加法，简化硬件实现，同时只有一个零的表示方式，是现代计算机整数表示的基础。',
       2,
       '补码,整数表示'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '补码表示法的一个重要优点是什么？');

INSERT INTO question (category_id, type, title, options, answer, analysis, difficulty, tags)
SELECT (SELECT id FROM question_category WHERE name = '计算机组成原理' ORDER BY id LIMIT 1), 4,
       '程序局部性通常分为时间局部性和____局部性。',
       NULL,
       '空间',
       '时间局部性指近期访问过的数据可能再次被访问；空间局部性指访问某地址后，附近地址也可能很快被访问。',
       1,
       '局部性,Cache'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE title = '程序局部性通常分为时间局部性和____局部性。');

-- 仅清理本次新增的判断题和填空题的 options 字段
UPDATE question SET options = NULL
WHERE title IN (
    'UDP 一定比 TCP 不安全，因此不能用于实时音视频。',
    '死锁只要出现循环等待就一定会发生。',
    '快速排序在任何输入下的时间复杂度都是 O(n log n)。',
    'InnoDB 的主键索引叶子节点存储的是完整行数据。',
    'Java 的 synchronized 既可以修饰实例方法，也可以修饰静态方法。',
    '适配器模式的目标是让原本接口不兼容的对象可以协同工作。',
    '并查集通常支持两个核心操作：查找根节点的 ____ 和合并集合的 ____。',
    '程序局部性通常分为时间局部性和____局部性。'
) AND type IN (3, 4);
